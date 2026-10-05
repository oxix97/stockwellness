package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class EodSchemaMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;
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
    void insertStock() {
        jdbc.update("DELETE FROM stock_ai_analysis_eod");
        jdbc.update("DELETE FROM stock_price_adjustment_eod");
        jdbc.update("DELETE FROM stock_technical_indicator_eod");
        jdbc.update("DELETE FROM stock_price_eod");
        jdbc.update("DELETE FROM stock_investor_trade_eod");
        jdbc.update("DELETE FROM stock");
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at,
                                 market_type, status, ticker, name)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'ACTIVE', '005930', 'Samsung')
                RETURNING id
                """, Long.class);
    }

    @Test
    void usesThreeColumnPrimaryKey_andAllowsDifferentScopes_butRejectsDuplicateNaturalKey() {
        List<String> pkColumns = jdbc.query("""
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name AND tc.constraint_schema = kcu.constraint_schema
                WHERE tc.table_name = 'stock_price_eod' AND tc.constraint_type = 'PRIMARY KEY'
                ORDER BY kcu.ordinal_position
                """, (rs, row) -> rs.getString(1));
        assertThat(pkColumns).containsExactly("stock_id", "trade_date", "quote_scope");

        insertPrice("KRX");
        insertPrice("UNIFIED");
        assertThatThrownBy(() -> insertPrice("KRX")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void enforcesFinalCloseHighLowAndCompositeForeignKey() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO stock_price_eod(stock_id, trade_date, quote_scope, source_api, data_status)
                VALUES (?, DATE '2026-10-02', 'KRX', 'TEST', 'FINAL')
                """, stockId)).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO stock_price_eod(stock_id, trade_date, quote_scope, high_price, low_price, close_price, source_api, data_status)
                VALUES (?, DATE '2026-10-02', 'KRX', 90, 100, 95, 'TEST', 'FINAL')
                """, stockId)).isInstanceOf(DataAccessException.class);

        insertPrice("KRX");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO stock_price_adjustment_eod(stock_id, trade_date, quote_scope, source_api, data_status)
                VALUES (?, DATE '2026-10-03', 'KRX', 'TEST', 'INCOMPLETE')
                """, stockId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void allowsNegativeFlowAndRequiresAllFourValuesForFinal() {
        jdbc.update("""
                INSERT INTO stock_investor_trade_eod(stock_id, trade_date, quote_scope, foreign_net_quantity,
                    institution_net_quantity, foreign_net_amount, institution_net_amount, data_status, source_api)
                VALUES (?, DATE '2026-10-02', 'KRX', -5, -2, -100.25, -50, 'FINAL', 'TEST')
                """, stockId);
        assertThat(jdbc.queryForObject("SELECT foreign_net_amount FROM stock_investor_trade_eod", java.math.BigDecimal.class))
                .isEqualByComparingTo("-100.25");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO stock_investor_trade_eod(stock_id, trade_date, quote_scope, foreign_net_quantity,
                    institution_net_quantity, foreign_net_amount, data_status, source_api)
                VALUES (?, DATE '2026-10-03', 'KRX', 1, 1, 10, 'FINAL', 'TEST')
                """, stockId)).isInstanceOf(DataAccessException.class);
    }

    private void insertPrice(String scope) {
        jdbc.update("""
                INSERT INTO stock_price_eod(stock_id, trade_date, quote_scope, close_price, source_api, data_status)
                VALUES (?, DATE '2026-10-02', ?, 100, 'TEST', 'FINAL')
                """, stockId, scope);
    }
}
