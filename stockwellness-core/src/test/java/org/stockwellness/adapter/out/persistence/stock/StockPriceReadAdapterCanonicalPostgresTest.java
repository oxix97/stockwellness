package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.support.ManualSqlTestSupport;

@Testcontainers(disabledWithoutDocker = true)
class StockPriceReadAdapterCanonicalPostgresTest {
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 11, 10);
    private static final LocalDate FINAL_DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDate PROVISIONAL_DATE = LocalDate.of(2026, 11, 12);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static StockPriceReadAdapter prices;
    private Long stockId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        prices = new StockPriceReadAdapter(new NamedParameterJdbcTemplate(dataSource));
    }

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM stock_technical_indicator");
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, status)
                VALUES ('KOSPI', '005930', '삼성전자', 'KRW', 'ACTIVE') RETURNING id
                """, Long.class);
    }

    @Test
    void history_uses_final_rows_previous_final_close_adjustment_and_current_indicators() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertPrice(FINAL_DATE, "110", "FINAL", 3);
        insertPrice(PROVISIONAL_DATE, "120", "PROVISIONAL", 1);
        jdbc.update("""
                INSERT INTO stock_price_adjustment(stock_id, trade_date, quote_scope, adjustment_basis,
                    adjustment_as_of, open_price, high_price, low_price, adjusted_close, source, collected_at)
                VALUES (?, ?, 'KRX', 'PROVIDER_ADJUSTED', ?, 108, 109, 107, 108.5, 'KIS:test', ?)
                """, stockId, FINAL_DATE, FINAL_DATE, java.sql.Timestamp.from(Instant.now()));
        insertIndicator(FINAL_DATE, manifest(FINAL_DATE, 3));

        List<?> history = prices.history(Set.of(stockId), "KRX", FINAL_DATE, PROVISIONAL_DATE, PriceBasis.RAW)
                .get(stockId);

        assertThat(history).hasSize(1);
        var finalPrice = (org.stockwellness.application.port.out.stock.PriceObservation) history.getFirst();
        assertThat(finalPrice.tradeDate()).isEqualTo(FINAL_DATE);
        assertThat(finalPrice.closePrice()).isEqualByComparingTo("110");
        assertThat(finalPrice.previousClosePrice()).isEqualByComparingTo("100");
        assertThat(finalPrice.adjustedClosePrice()).isEqualByComparingTo("108.5");
        assertThat(finalPrice.ma5()).isEqualByComparingTo("105");
    }

    @Test
    void recent_returns_only_final_rows_in_descending_date_order() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertPrice(FINAL_DATE, "110", "FINAL", 1);
        insertPrice(PROVISIONAL_DATE, "120", "PROVISIONAL", 1);

        var history = prices.recent(Set.of(stockId), "KRX", PROVISIONAL_DATE, 2, PriceBasis.RAW).get(stockId);

        assertThat(history).extracting("tradeDate").containsExactly(FINAL_DATE, FIRST_DATE);
        assertThat(history.getFirst().previousClosePrice()).isEqualByComparingTo("100");
    }

    @Test
    void history_ignores_indicator_when_a_source_revision_is_stale() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertPrice(FINAL_DATE, "110", "FINAL", 3);
        insertIndicator(FINAL_DATE, manifest(FINAL_DATE, 3));
        jdbc.update("UPDATE stock_price_daily SET revision = 2 WHERE stock_id = ? AND trade_date = ?",
                stockId, FIRST_DATE);

        var finalPrice = prices.history(Set.of(stockId), "KRX", FINAL_DATE, FINAL_DATE, PriceBasis.RAW)
                .get(stockId).getFirst();

        assertThat(finalPrice.ma5()).isNull();
    }

    @Test
    void history_and_recent_require_market_scope_and_price_basis() {
        assertThatThrownBy(() -> prices.history(Set.of(stockId), " ", FINAL_DATE, PROVISIONAL_DATE, PriceBasis.RAW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> prices.history(Set.of(stockId), "KRX", FINAL_DATE, PROVISIONAL_DATE, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> prices.recent(Set.of(stockId), "KRX", PROVISIONAL_DATE, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void insertPrice(LocalDate date, String close, String status, int revision) {
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, volume, trading_amount, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', ?, ?, ?, ?, 100, 1000, ?, 'KIS:test', ?, ?)
                """, stockId, date, new java.math.BigDecimal(close), new java.math.BigDecimal(close),
                new java.math.BigDecimal(close), new java.math.BigDecimal(close), status,
                java.sql.Timestamp.from(Instant.now()), revision);
    }

    private void insertIndicator(LocalDate date, String manifest) {
        jdbc.update("""
                INSERT INTO stock_technical_indicator(stock_id, trade_date, quote_scope, price_basis, ma5, ma20,
                    ma20_disparity, ma60, ma120, rsi14, macd, macd_signal, adx, plus_di, minus_di,
                    input_manifest, algorithm_version, calculated_at)
                VALUES (?, ?, 'KRX', 'RAW', 105, 102, 100, 99, 98, 55, 1, 0.5, 20, 25, 15,
                    ?::jsonb, 'test-v1', ?)
                """, stockId, date, manifest, java.sql.Timestamp.from(Instant.now()));
    }

    private String manifest(LocalDate targetDate, int targetRevision) {
        return """
                {"source":"stock_price_daily","sourceId":%d,"quoteScope":"KRX","priceBasis":"RAW",
                 "targetDate":"%s","observations":[{"date":"%s","revision":1},
                 {"date":"%s","revision":%d}]}
                """.formatted(stockId, targetDate, FIRST_DATE, targetDate, targetRevision);
    }
}
