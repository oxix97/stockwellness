package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import jakarta.persistence.EntityManager;
import org.stockwellness.adapter.out.persistence.stock.repository.EodStockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexDailyRepository;
import org.stockwellness.adapter.out.persistence.insight.IndexDailyAdapter;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.support.ManualSqlTestSupport;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@EnableJpaRepositories(basePackageClasses = EodStockPriceRepository.class)
@Import({EodStockPriceAdapter.class, IndexDailyAdapter.class, QueryDslConfig.class})
class EodStockPricePostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        ManualSqlTestSupport.initializeLegacyPostgres(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired EodStockPriceAdapter adapter;
    @Autowired EodStockPriceRepository repository;
    @Autowired IndexDailyRepository indexDailyRepository;
    @Autowired IndexDailyAdapter indexDailyAdapter;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    private Long stockId;
    private Long indexId;
    private final LocalDate tradeDate = LocalDate.of(2026, 10, 2);

    @BeforeEach
    void insertStock() {
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM market_index");
        indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_code, index_name, provider, provider_code, index_kind, created_at)
                VALUES ('0001', 'KOSPI', 'KIS', '0001', 'MARKET', CURRENT_TIMESTAMP) RETURNING id
                """, Long.class);
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at, market_type, status, ticker, name)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'ACTIVE', '005930', 'Samsung') RETURNING id
                """, Long.class);
    }

    @Test
    void upsert_is_idempotent_and_revisions_only_changed_values() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(quote("100"))));
        tx.executeWithoutResult(status -> adapter.upsert(List.of(quote("100.0000"))));
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT corrected_at FROM stock_price_daily", java.sql.Timestamp.class)).isNull();

        tx.executeWithoutResult(status -> adapter.upsert(List.of(quote("101"))));
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT corrected_at FROM stock_price_daily", java.sql.Timestamp.class)).isNotNull();
    }

    @Test
    void concurrent_corrections_serialize_on_the_existing_natural_key() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(quote("100"))));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (String close : List.of("101", "102")) {
                executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> adapter.upsert(List.of(quote(close))));
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        }
        assertThat(repository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(3);
    }

    @Test
    void concurrent_first_insert_for_the_same_natural_key_creates_one_row_and_keeps_the_correction() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (String close : List.of("101", "102")) {
                executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> adapter.upsert(List.of(quote(close))));
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        }
        assertThat(repository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_daily", BigDecimal.class))
                .isIn(new BigDecimal("101.0000"), new BigDecimal("102.0000"));
    }

    @Test
    void incomplete_retry_cannot_downgrade_a_final_observation() {
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', 100, 110, 90, 105, 'FINAL', 'KIS:verified', CURRENT_TIMESTAMP, 1)
                """, stockId, tradeDate);

        adapter.upsert(List.of(EodStockPrice.of(entityManager.find(Stock.class, stockId), tradeDate, "KRX",
                null, null, null, null, null, null, "KIS:daily:RAW", "INCOMPLETE")));

        assertThat(jdbc.queryForObject("SELECT data_status FROM stock_price_daily", String.class)).isEqualTo("FINAL");
        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_daily", BigDecimal.class))
                .isEqualByComparingTo("105");
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(1);
    }

    @Test
    void provider_adjustment_is_stored_separately_with_observation_as_of_date() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(quote("100"))));
        Stock stock = entityManager.find(Stock.class, stockId);
        StockPriceAdjustment adjustment = StockPriceAdjustment.of(stock, tradeDate, "KRX", tradeDate.plusDays(3),
                new BigDecimal("90"), new BigDecimal("99"), new BigDecimal("81"), new BigDecimal("95"),
                "KIS:FHKST03010100:ADJUSTED");

        tx.executeWithoutResult(status -> adapter.upsertAdjustments(List.of(adjustment)));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_daily", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_adjustment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT adjusted_close FROM stock_price_adjustment", BigDecimal.class))
                .isEqualByComparingTo("95");
        assertThat(jdbc.queryForObject("SELECT adjustment_as_of FROM stock_price_adjustment", java.sql.Date.class)
                .toLocalDate()).isEqualTo(tradeDate.plusDays(3));
    }

    @Test
    void combined_price_and_adjustment_write_rolls_back_raw_when_adjustment_insert_fails() {
        jdbc.execute("ALTER TABLE stock_price_adjustment ADD CONSTRAINT test_reject_adjustment CHECK (source <> 'FAIL')");
        try {
            Stock stock = entityManager.find(Stock.class, stockId);
            StockPriceAdjustment invalid = StockPriceAdjustment.of(stock, tradeDate, "KRX", tradeDate.plusDays(1),
                    new BigDecimal("90"), new BigDecimal("99"), new BigDecimal("81"), new BigDecimal("95"), "FAIL");

            assertThatThrownBy(() -> adapter.upsertObservations(List.of(quote("100")), List.of(invalid)))
                    .isInstanceOf(DataAccessException.class);

            assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_daily", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_adjustment", Integer.class)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE stock_price_adjustment DROP CONSTRAINT test_reject_adjustment");
        }
    }

    @Test
    void index_price_upsert_revises_prices_and_leaves_unsupported_supply_fields_null() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(indexQuote("2500"))));
        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(indexQuote("2500.0000"))));
        assertThat(jdbc.queryForObject("SELECT data_revision FROM index_daily", Integer.class)).isEqualTo(1);

        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(indexQuote("2501"))));
        assertThat(indexDailyRepository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT data_revision FROM index_daily", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT foreign_net_amount FROM index_daily", BigDecimal.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT supply_status FROM index_daily", String.class)).isNull();
    }

    private EodStockPrice quote(String close) {
        Stock stock = entityManager.find(Stock.class, stockId);
        return EodStockPrice.of(stock, tradeDate, "KRX", new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("90"), new BigDecimal(close), 1L, new BigDecimal("1000"),
                "KIS:FHKST03010100:RAW", "PROVISIONAL");
    }

    private IndexDaily indexQuote(String close) {
        MarketIndex index = entityManager.find(MarketIndex.class, indexId);
        return IndexDaily.of(index, tradeDate, new BigDecimal("2490"), new BigDecimal("2510"),
                new BigDecimal("2480"), new BigDecimal(close), 100L, new BigDecimal("100000"),
                null, null, null, null, null, null, null, "FHKUP03500100", null,
                BenchmarkDataStatus.PROVISIONAL, null);
    }
}
