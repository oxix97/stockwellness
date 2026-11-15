package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.domain.stock.price.EodStockInvestorTradeDaily;
import org.stockwellness.domain.stock.price.InvestorSourceType;
import org.stockwellness.domain.stock.price.InvestorType;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@JdbcTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@Import(EodStockInvestorTradeAdapter.class)
class EodStockInvestorTradePostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 10, 5);
    private static final Instant COLLECTED_AT = Instant.parse("2026-10-05T07:00:00Z");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        ManualSqlTestSupport.initializeRedesignedPostgres(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private EodStockInvestorTradeAdapter adapter;
    @Autowired private JdbcTemplate jdbc;
    private long firstStockId;

    @BeforeEach
    void insertStockWithoutAnyPriceRow() {
        jdbc.update("DELETE FROM stock_investor_trade_daily");
        jdbc.update("DELETE FROM stock");
        firstStockId = stock("005930");
    }

    @Test
    void upsert_uses_the_natural_key_is_idempotent_revises_corrections_and_protects_final_data() {
        adapter.upsert(List.of(row(firstStockId, InvestorSourceType.FOREIGN, "-10", "100", "PROVISIONAL")));
        adapter.upsert(List.of(new EodStockInvestorTradeDaily(firstStockId, TRADE_DATE, "KRX",
                InvestorSourceType.FOREIGN, new BigDecimal("-10.000000"), new BigDecimal("100.00"),
                "PROVISIONAL", "KIS:FHPTJ04160001:J", COLLECTED_AT.plusSeconds(60))));
        adapter.upsert(List.of(row(firstStockId, InvestorSourceType.FOREIGN, null, null, "INCOMPLETE")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_investor_trade_daily", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT data_status FROM stock_investor_trade_daily", String.class)).isEqualTo("PROVISIONAL");
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_investor_trade_daily", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT corrected_at FROM stock_investor_trade_daily", java.sql.Timestamp.class)).isNull();

        adapter.upsert(List.of(row(firstStockId, InvestorSourceType.FOREIGN, "-10", "101", "PROVISIONAL")));
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_investor_trade_daily", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT corrected_at FROM stock_investor_trade_daily", java.sql.Timestamp.class)).isNotNull();

        adapter.upsert(List.of(row(firstStockId, InvestorSourceType.FOREIGN, "-10", "101", "FINAL")));
        adapter.upsert(List.of(row(firstStockId, InvestorSourceType.FOREIGN, null, null, "INCOMPLETE")));
        assertThat(jdbc.queryForObject("SELECT data_status FROM stock_investor_trade_daily", String.class)).isEqualTo("FINAL");
        assertThat(jdbc.queryForObject("SELECT net_amount FROM stock_investor_trade_daily", BigDecimal.class))
                .isEqualByComparingTo("101");
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_investor_trade_daily", Integer.class)).isEqualTo(3);
    }

    @Test
    void ranking_uses_final_totals_only_and_all_sums_foreign_plus_institution_total_once() {
        long secondStockId = stock("000660");
        adapter.upsert(List.of(
                row(firstStockId, InvestorSourceType.FOREIGN, "1", "4", "FINAL"),
                row(firstStockId, InvestorSourceType.INSTITUTION_TOTAL, "2", "6", "FINAL"),
                row(firstStockId, InvestorSourceType.FINANCIAL_INVESTMENT, "3", "100", "FINAL"),
                row(secondStockId, InvestorSourceType.FOREIGN, "5", "20", "FINAL"),
                row(secondStockId, InvestorSourceType.INSTITUTION_TOTAL, null, null, "INCOMPLETE")));

        assertThat(adapter.findRanked(TRADE_DATE, "KRX", InvestorType.ALL, 10))
                .singleElement().satisfies(ranking -> {
                    assertThat(ranking.stockId()).isEqualTo(firstStockId);
                    assertThat(ranking.netQuantity()).isEqualByComparingTo("3");
                    assertThat(ranking.netAmount()).isEqualByComparingTo("10");
                });
        assertThat(adapter.findRanked(TRADE_DATE, "KRX", InvestorType.FOREIGN, 10))
                .extracting(ranking -> ranking.stockId()).containsExactly(secondStockId, firstStockId);
    }

    @Test
    void bulk_write_rolls_back_all_rows_when_one_quote_scope_violates_the_schema() {
        EodStockInvestorTradeDaily invalid = new EodStockInvestorTradeDaily(firstStockId, TRADE_DATE, "INVALID",
                InvestorSourceType.INSTITUTION_TOTAL, BigDecimal.ONE, BigDecimal.ONE,
                "PROVISIONAL", "KIS:FHPTJ04160001:J", COLLECTED_AT);

        assertThatThrownBy(() -> adapter.upsert(List.of(
                row(firstStockId, InvestorSourceType.FOREIGN, "1", "1", "PROVISIONAL"), invalid)))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_investor_trade_daily", Integer.class)).isZero();
    }

    private long stock(String ticker) {
        return jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, status)
                VALUES ('KOSPI', ?, ?, 'KRW', 'ACTIVE') RETURNING id
                """, Long.class, ticker, ticker);
    }

    private static EodStockInvestorTradeDaily row(long stockId, InvestorSourceType type,
            String quantity, String amount, String status) {
        return new EodStockInvestorTradeDaily(stockId, TRADE_DATE, "KRX", type,
                quantity == null ? null : new BigDecimal(quantity),
                amount == null ? null : new BigDecimal(amount), status,
                "KIS:FHPTJ04160001:J", COLLECTED_AT);
    }
}
