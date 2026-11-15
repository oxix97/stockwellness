package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.support.ManualSqlTestSupport;

@Testcontainers(disabledWithoutDocker = true)
class StockPriceReadPostgresTest {
    private static final LocalDate FIRST = LocalDate.of(2026, 10, 1);
    private static final LocalDate SECOND = FIRST.plusDays(1);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static StockPriceReadPort readPort;
    private long stockId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(
                dataSource);
        jdbc = new JdbcTemplate(dataSource);
        readPort = new StockPriceReadAdapter(new NamedParameterJdbcTemplate(dataSource));
    }

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM stock_technical_indicator");
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, status)
                VALUES ('KOSPI', '005930', 'Samsung', 'KRW', 'ACTIVE') RETURNING id
                """, Long.class);
    }

    @Test
    void raw_history_returns_only_final_observations_within_the_requested_dates() {
        insertRaw(FIRST, "FINAL", "100");
        insertRaw(SECOND, "PROVISIONAL", "110");

        var result = readPort.history(Set.of(stockId), "KRX", FIRST, SECOND, PriceBasis.RAW);

        assertThat(result.get(stockId)).hasSize(1);
        assertThat(result.get(stockId).getFirst().tradeDate()).isEqualTo(FIRST);
        assertThat(result.get(stockId).getFirst().closePrice()).isEqualByComparingTo("100");
    }

    @Test
    void adjusted_history_does_not_fall_back_to_raw_when_adjustment_is_missing() {
        insertRaw(FIRST, "FINAL", "100");

        var result = readPort.history(Set.of(stockId), "KRX", FIRST, FIRST, PriceBasis.PROVIDER_ADJUSTED);

        assertThat(result).containsEntry(stockId, java.util.List.of());
    }

    @Test
    void adjusted_history_returns_provider_adjusted_close_when_present() {
        insertRaw(FIRST, "FINAL", "100");
        jdbc.update("""
                INSERT INTO stock_price_adjustment(stock_id, trade_date, quote_scope, adjustment_basis,
                    adjustment_as_of, adjusted_close, source, collected_at)
                VALUES (?, ?, 'KRX', 'PROVIDER_ADJUSTED', ?, 95, 'KIS:adjusted', CURRENT_TIMESTAMP)
                """, stockId, FIRST, FIRST.plusDays(1));

        var result = readPort.history(Set.of(stockId), "KRX", FIRST, FIRST, PriceBasis.PROVIDER_ADJUSTED);

        assertThat(result.get(stockId)).hasSize(1);
        assertThat(result.get(stockId).getFirst().closePrice()).isEqualByComparingTo("95");
        assertThat(result.get(stockId).getFirst().priceBasis()).isEqualTo(PriceBasis.PROVIDER_ADJUSTED);
    }

    @Test
    void recent_returns_latest_final_observations_in_descending_order_and_respects_as_of() {
        insertRaw(FIRST, "FINAL", "100");
        insertRaw(SECOND, "FINAL", "110");
        insertRaw(SECOND.plusDays(1), "FINAL", "120");

        var result = readPort.recent(Set.of(stockId), "KRX", SECOND, 2, PriceBasis.RAW);

        assertThat(result.get(stockId)).extracting(PriceObservation::tradeDate).containsExactly(SECOND, FIRST);
        assertThat(readPort.latest(Set.of(stockId), "KRX", SECOND, PriceBasis.RAW).get(stockId).tradeDate())
                .isEqualTo(SECOND);
    }

    private void insertRaw(LocalDate tradeDate, String status, String close) {
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, volume, trading_amount, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', 90, 200, 80, ?, 1000, 100000, ?, 'KIS:raw', CURRENT_TIMESTAMP, 1)
                """, stockId, tradeDate, new BigDecimal(close), status);
    }
}
