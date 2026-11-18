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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.support.ManualSqlTestSupport;
import org.stockwellness.domain.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@EntityScan(basePackageClasses = {Stock.class, EodStockPrice.class, IndexDaily.class, OutboxEvent.class})
@EnableJpaRepositories(basePackageClasses = EodStockPriceRepository.class)
@Import({EodStockPriceAdapter.class, IndexDailyAdapter.class, QueryDslConfig.class, EodStockPricePostgresTest.JacksonTestConfig.class})
class EodStockPricePostgresTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class JacksonTestConfig {
        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

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
    @MockitoBean OutboxPort outboxPort;
    private Long stockId;
    private Long indexId;
    private Long seriesId;
    private final LocalDate tradeDate = LocalDate.of(2026, 10, 2);

    @BeforeEach
    void insertStock() {
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM index_series");
        jdbc.update("DELETE FROM market_index");
        indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_code, index_name, provider, provider_code, index_kind, created_at)
                VALUES ('0001', 'KOSPI', 'KIS', '0001', 'MARKET', CURRENT_TIMESTAMP) RETURNING id
                """, Long.class);
        seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                                         calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.idxcode.KOSPI', '0001', 'KOSPI', 'PRICE', 'NONE', 'KRW', true)
                RETURNING id
                """, Long.class, indexId);
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
    void final_price_correction_emits_one_revision_event_and_identical_replay_emits_none() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("100"))));
        tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("100.0000"))));
        verify(outboxPort, never()).save(any());

        tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("101"))));

        org.mockito.ArgumentCaptor<OutboxEvent> event = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxPort).save(event.capture());
        assertThat(event.getValue().getTopic()).isEqualTo("market-data-revision");
        assertThat(event.getValue().getPayload())
                .contains("\"sourceType\":\"STOCK_PRICE\"", "\"sourceId\":" + stockId,
                        "\"tradeDate\":\"" + tradeDate + "\"", "\"revision\":2");
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(2);
    }

    @Test
    void batched_final_price_write_emits_events_only_for_changed_existing_rows() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("100"))));
        Long secondStockId = jdbc.queryForObject("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at, market_type, status, ticker, name)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'ACTIVE', '005931', 'Second') RETURNING id
                """, Long.class);

        tx.executeWithoutResult(status -> adapter.upsert(List.of(
                finalQuote("101"), finalQuote(entityManager.find(Stock.class, secondStockId), "105"))));

        verify(outboxPort).save(any());
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily WHERE stock_id = ?",
                Integer.class, stockId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily WHERE stock_id = ?",
                Integer.class, secondStockId)).isEqualTo(1);
    }

    @Test
    void outbox_failure_rolls_back_a_final_price_correction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("100"))));
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxPort).save(any());

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> adapter.upsert(List.of(finalQuote("101")))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_daily", BigDecimal.class))
                .isEqualByComparingTo("100");
        assertThat(jdbc.queryForObject("SELECT revision FROM stock_price_daily", Integer.class)).isEqualTo(1);
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
        assertThat(jdbc.queryForObject("SELECT revision FROM index_daily", Integer.class)).isEqualTo(1);

        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(indexQuote("2501"))));
        assertThat(indexDailyRepository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT revision FROM index_daily", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT foreign_net_amount FROM index_daily", BigDecimal.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT supply_status FROM index_daily", String.class)).isNull();
    }

    @Test
    void final_index_price_correction_emits_revision_event_but_supply_only_change_does_not() {
        insertFinalIndexRow();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(finalIndexQuote("2500", "1"))));
        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(finalIndexQuote("2500.0000", "1"))));
        verify(outboxPort, never()).save(any());

        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(finalIndexQuote("2501", "1"))));
        tx.executeWithoutResult(status -> indexDailyAdapter.upsert(List.of(finalIndexQuote("2501", "2"))));

        org.mockito.ArgumentCaptor<OutboxEvent> event = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxPort).save(event.capture());
        assertThat(event.getValue().getTopic()).isEqualTo("market-data-revision");
        assertThat(event.getValue().getPayload()).contains("\"sourceType\":\"INDEX_PRICE\"",
                "\"sourceId\":" + seriesId, "\"revision\":2");
    }

    private EodStockPrice quote(String close) {
        Stock stock = entityManager.find(Stock.class, stockId);
        return EodStockPrice.of(stock, tradeDate, "KRX", new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("90"), new BigDecimal(close), 1L, new BigDecimal("1000"),
                "KIS:FHKST03010100:RAW", "PROVISIONAL");
    }

    private EodStockPrice finalQuote(String close) {
        Stock stock = entityManager.find(Stock.class, stockId);
        return finalQuote(stock, close);
    }

    private EodStockPrice finalQuote(Stock stock, String close) {
        return EodStockPrice.of(stock, tradeDate, "KRX", new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("90"), new BigDecimal(close), 1L, new BigDecimal("1000"),
                "KIS:FHKST03010100:RAW", "FINAL");
    }

    private IndexDaily indexQuote(String close) {
        IndexSeries series = entityManager.find(IndexSeries.class, seriesId);
        return IndexDaily.of(series, tradeDate, new BigDecimal("2490"), new BigDecimal("2510"),
                new BigDecimal("2480"), new BigDecimal(close), 100L, new BigDecimal("100000"),
                null, null, null, null, null, null, null, "FHKUP03500100", null,
                BenchmarkDataStatus.PROVISIONAL, null);
    }

    private void insertFinalIndexRow() {
        jdbc.update("""
                INSERT INTO index_daily(index_id, series_id, trade_date, open_value, high_value, low_value,
                    close_value, open_price, high_price, low_price, close_price, volume, trading_amount, foreign_net_amount,
                    institution_net_amount, price_source, price_source_api, supply_source,
                    supply_source_api, price_status, supply_status, collected_at, supply_collected_at,
                    revision, supply_revision)
                VALUES (?, ?, ?, 2490, 2510, 2480, 2500, 2490, 2510, 2480, 2500, 100, 100000, 1, 0,
                    'FHKUP03500100', 'FHKUP03500100', 'KIS:SUPPLY', 'KIS:SUPPLY', 'FINAL',
                    'PROVISIONAL', now(), now(), 1, 1)
                """, indexId, seriesId, tradeDate);
    }

    private IndexDaily finalIndexQuote(String close, String foreignNetAmount) {
        IndexSeries series = entityManager.find(IndexSeries.class, seriesId);
        return IndexDaily.of(series, tradeDate, new BigDecimal("2490"), new BigDecimal("2510"),
                new BigDecimal("2480"), new BigDecimal(close), 100L, new BigDecimal("100000"),
                null, null, null, null, null, foreignNetAmount == null ? null : new BigDecimal(foreignNetAmount),
                foreignNetAmount == null ? null : BigDecimal.ZERO,
                "FHKUP03500100", foreignNetAmount == null ? null : "KIS:SUPPLY",
                BenchmarkDataStatus.FINAL,
                foreignNetAmount == null ? null : BenchmarkDataStatus.PROVISIONAL);
    }
}
