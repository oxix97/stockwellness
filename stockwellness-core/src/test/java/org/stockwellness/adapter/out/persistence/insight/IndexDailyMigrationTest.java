package org.stockwellness.adapter.out.persistence.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class IndexDailyMigrationTest {
    private static final String TRADE_DATE = "2026-10-02";
    private static final String PRICE_DATE = "2026-10-01";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;
    private static Long indexId;
    private static Long stockId;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @BeforeEach
    void seedSourceAndAnalysis() {
        jdbc.update("DELETE FROM sector_leading_stock");
        jdbc.update("DELETE FROM sector_analysis");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM stock_price_eod");
        jdbc.update("DELETE FROM stock");
        jdbc.update("DELETE FROM market_index");
        indexId = jdbc.queryForObject("""
                INSERT INTO market_index(created_at, index_code, index_name, provider, provider_code, index_kind)
                VALUES (CURRENT_TIMESTAMP, 'KOSPI', '코스피', 'KIS', '0001', 'MARKET')
                RETURNING id
                """, Long.class);
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at,
                                 market_type, market_code, status, ticker, name)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'KOSPI', 'ACTIVE', '005930', 'Samsung')
                RETURNING id
                """, Long.class);
        jdbc.update("""
                INSERT INTO stock_price_eod(stock_id, trade_date, quote_scope, close_price, source_api, data_status)
                VALUES (?, DATE '2026-10-01', 'UNIFIED', 100, 'TEST', 'FINAL')
                """, stockId);
        jdbc.update("""
                INSERT INTO index_daily(index_id, trade_date, close_price, price_source_api, price_status,
                                       supply_source_api, supply_status)
                VALUES (?, DATE '2026-10-02', 100, 'PRICE_TEST', 'FINAL', 'SUPPLY_TEST', 'INCOMPLETE')
                """, indexId);
        jdbc.update("""
                INSERT INTO sector_analysis(index_id, trade_date, advance_ratio_unavailable_reason,
                                            calculation_version, input_data_version)
                VALUES (?, DATE '2026-10-02', 'SOURCE_COUNTS_UNAVAILABLE', 'test-v1', 'input-v1')
                """, indexId);
    }

    @Test
    void rejectsDuplicateSourceDateAndKeepsPriceAndSupplyStatusesIndependent() {
        assertThat(jdbc.queryForObject("""
                SELECT price_status || ':' || supply_status FROM index_daily
                WHERE index_id = ? AND trade_date = DATE '2026-10-02'
                """, String.class, indexId)).isEqualTo("FINAL:INCOMPLETE");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO index_daily(index_id, trade_date, price_source_api, price_status)
                VALUES (?, DATE '2026-10-02', 'OTHER', 'INCOMPLETE')
                """, indexId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void enforcesLeaderRankActualQuoteDateAndOneStockPerIndexDate() {
        insertLeader(1, PRICE_DATE);

        assertThatThrownBy(() -> insertLeader(2, PRICE_DATE)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertLeader(6, PRICE_DATE)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertLeader(3, "2026-10-03")).isInstanceOf(DataAccessException.class);
        Long stockWithoutQuote = insertStock("005961");
        assertThatThrownBy(() -> insertLeader(4, PRICE_DATE, stockWithoutQuote, "UNIFIED"))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT price_date::text FROM sector_leading_stock", String.class))
                .startsWith("2026-10-01");
    }

    private void insertLeader(int rank, String priceDate) {
        insertLeader(rank, priceDate, stockId, "UNIFIED");
    }

    private void insertLeader(int rank, String priceDate, Long leaderStockId, String quoteScope) {
        jdbc.update("""
                INSERT INTO sector_leading_stock(index_id, trade_date, rank, stock_id, price_date, quote_scope)
                VALUES (?, DATE '2026-10-02', ?, ?, ?::date, ?)
                """, indexId, rank, leaderStockId, priceDate, quoteScope);
    }

    private Long insertStock(String ticker) {
        return jdbc.queryForObject("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at,
                                 market_type, market_code, status, ticker, name)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'KOSPI', 'ACTIVE', ?, 'Test')
                RETURNING id
                """, Long.class, ticker);
    }
}
