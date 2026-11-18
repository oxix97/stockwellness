package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.application.service.batch.BenchmarkPriceSyncService;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalBenchmarkPriceAdapterPostgresTest {
    private static final LocalDate PRIOR_VALID_DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDate INCOMPLETE_NULL_DATE = LocalDate.of(2026, 11, 12);
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 13);
    private static final LocalDate FUTURE_DATE = LocalDate.of(2026, 11, 14);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static CanonicalBenchmarkPriceAdapter adapter;
    private Long seriesId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        adapter = new CanonicalBenchmarkPriceAdapter(
                jdbc, mock(IndexSeriesRepository.class), mock(IndexDailyPort.class));
    }

    @BeforeEach
    void seedCanonicalSeries() {
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM index_series");
        jdbc.update("DELETE FROM market_index");
        Long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('KOSPI:2001', '코스피 200', 'MARKET', '2001') RETURNING id
                """, Long.class);
        seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical)
                VALUES (?, 'KIS', 'kis.idxcode.KOSPI', '2001', 'KOSPI', 'PRICE', 'NONE', 'KRW', true, true)
                RETURNING id
                """, Long.class, indexId);
    }

    @Test
    void findLatestBefore_skips_incomplete_null_close_row_and_returns_prior_provisional_close() {
        insertIndexDaily(PRIOR_VALID_DATE, new BigDecimal("100.0000"), "PROVISIONAL");
        insertIndexDaily(INCOMPLETE_NULL_DATE, null, "INCOMPLETE");
        insertIndexDaily(TARGET_DATE, new BigDecimal("115.0000"), "PROVISIONAL");
        insertIndexDaily(FUTURE_DATE, new BigDecimal("120.0000"), "FINAL");

        BenchmarkPrice previous = adapter.findLatestBefore("2001", TARGET_DATE).orElseThrow();

        assertThat(previous.getBaseDate()).isEqualTo(PRIOR_VALID_DATE);
        assertThat(previous.getClosePrice()).isEqualByComparingTo("100.0000");

        var synced = new BenchmarkPriceSyncService(adapter).toBenchmarkPrice(
                new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                        BenchmarkType.KOSPI_200,
                        TARGET_DATE,
                        new BigDecimal("104"),
                        new BigDecimal("106"),
                        new BigDecimal("103"),
                        new BigDecimal("105"),
                        null,
                        1000L));
        assertThat(synced.benchmarkPrice().getChangeRate()).isEqualByComparingTo("5.000000");
    }

    @Test
    void findLatestBefore_returns_empty_when_only_incomplete_null_close_rows_exist_before_base_date() {
        insertIndexDaily(INCOMPLETE_NULL_DATE, null, "INCOMPLETE");
        insertIndexDaily(TARGET_DATE, new BigDecimal("115.0000"), "PROVISIONAL");
        insertIndexDaily(FUTURE_DATE, new BigDecimal("120.0000"), "FINAL");

        assertThat(adapter.findLatestBefore("2001", TARGET_DATE)).isEmpty();
    }

    @Test
    void findLatestBefore_preserves_prior_final_row_with_change_rate_and_excludes_base_date_or_future() {
        insertIndexDaily(LocalDate.of(2026, 11, 10), new BigDecimal("100.0000"), "FINAL");
        insertIndexDaily(PRIOR_VALID_DATE, new BigDecimal("110.0000"), "FINAL");
        insertIndexDaily(TARGET_DATE, new BigDecimal("115.0000"), "FINAL");
        insertIndexDaily(FUTURE_DATE, new BigDecimal("120.0000"), "FINAL");

        BenchmarkPrice previous = adapter.findLatestBefore("2001", TARGET_DATE).orElseThrow();

        assertThat(previous.getBaseDate()).isEqualTo(PRIOR_VALID_DATE);
        assertThat(previous.getClosePrice()).isEqualByComparingTo("110.0000");
        assertThat(previous.getChangeRate()).isEqualByComparingTo("10.000000");
    }

    private void insertIndexDaily(LocalDate tradeDate, BigDecimal closeValue, String priceStatus) {
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, high_value, low_value, close_value,
                    volume, price_status, price_source, collected_at)
                VALUES (?, ?, ?, ?, ?, ?, 1000, ?, 'KIS:test', ?)
                """,
                seriesId,
                tradeDate,
                closeValue,
                closeValue,
                closeValue,
                closeValue,
                priceStatus,
                java.sql.Timestamp.from(Instant.now()));
    }
}
