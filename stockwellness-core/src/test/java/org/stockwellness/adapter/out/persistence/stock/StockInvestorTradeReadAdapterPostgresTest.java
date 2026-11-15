package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.stockwellness.domain.stock.price.TradeDirection;
import org.stockwellness.support.ManualSqlTestSupport;

@Testcontainers(disabledWithoutDocker = true)
class StockInvestorTradeReadAdapterPostgresTest {
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 11, 10);
    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDate FLOW_DATE = LocalDate.of(2026, 11, 12);
    private static final LocalDate PROVISIONAL_DATE = LocalDate.of(2026, 11, 13);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static StockInvestorTradeReadAdapter trades;
    private Long pricedStockId;
    private Long unpricedStockId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        trades = new StockInvestorTradeReadAdapter(jdbc);
    }

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM stock_investor_trade_daily");
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        jdbc.update("DELETE FROM market_index WHERE kind = 'SECTOR'");
        Long sectorId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind) VALUES ('sector-semiconductor', '반도체', 'SECTOR')
                RETURNING id
                """, Long.class);
        pricedStockId = insertStock("005930", sectorId);
        unpricedStockId = insertStock("000660", sectorId);
    }

    @Test
    void latest_date_and_rankings_use_final_supply_independently_of_price_date() {
        insertPrice(pricedStockId, FIRST_DATE, "100");
        insertPrice(pricedStockId, PRICE_DATE, "110");
        insertFlow(pricedStockId, FLOW_DATE, "FOREIGN", "10", "500", "FINAL");
        insertFlow(pricedStockId, FLOW_DATE, "INSTITUTION_TOTAL", "20", "1000", "FINAL");
        insertFlow(pricedStockId, FLOW_DATE, "FINANCIAL_INVESTMENT", "12", "600", "FINAL");
        insertFlow(unpricedStockId, FLOW_DATE, "FOREIGN", "30", "1500", "FINAL");
        insertFlow(unpricedStockId, FLOW_DATE, "INSTITUTION_TOTAL", "14", "700", "FINAL");
        insertFlow(pricedStockId, PROVISIONAL_DATE, "FOREIGN", "99", "9999", "PROVISIONAL");

        assertThat(trades.latestFinalDate("KRX")).contains(FLOW_DATE);

        var foreign = trades.topStocks(FLOW_DATE, "KRX", "FOREIGN", TradeDirection.BUY, 10);
        var institution = trades.topStocks(FLOW_DATE, "KRX", "INSTITUTION_TOTAL", TradeDirection.BUY, 10);

        assertThat(foreign).extracting("ticker").containsExactly("000660", "005930");
        assertThat(foreign.getFirst().currentPrice()).isNull();
        assertThat(foreign.getFirst().fluctuationRate()).isNull();
        assertThat(foreign.getFirst().sectorName()).isEqualTo("반도체");
        assertThat(foreign.getLast().currentPrice()).isEqualByComparingTo("110");
        assertThat(foreign.getLast().fluctuationRate()).isEqualByComparingTo("10.0");
        assertThat(institution.getFirst().netBuyingAmount()).isEqualByComparingTo("1000");
        assertThat(institution.getFirst().netBuyingQuantity()).isEqualTo(20L);
    }

    private Long insertStock(String ticker, Long sectorId) {
        return jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, sector_index_id, status)
                VALUES ('KOSPI', ?, ?, 'KRW', ?, 'ACTIVE') RETURNING id
                """, Long.class, ticker, ticker, sectorId);
    }

    private void insertPrice(Long stockId, LocalDate date, String close) {
        BigDecimal value = new BigDecimal(close);
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, volume, trading_amount, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', ?, ?, ?, ?, 100, 1000, 'FINAL', 'KIS:test', ?, 1)
                """, stockId, date, value, value, value, value, java.sql.Timestamp.from(Instant.now()));
    }

    private void insertFlow(Long stockId, LocalDate date, String investorType, String quantity,
            String amount, String status) {
        jdbc.update("""
                INSERT INTO stock_investor_trade_daily(stock_id, trade_date, quote_scope, investor_type,
                    net_quantity, net_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', ?, ?, ?, ?, 'KIS:test', ?)
                """, stockId, date, investorType, new BigDecimal(quantity), new BigDecimal(amount), status,
                java.sql.Timestamp.from(Instant.now()));
    }
}
