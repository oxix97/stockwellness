package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.domain.stock.price.StockTechnicalIndicator;
import org.stockwellness.domain.stock.insight.IndexTechnicalIndicator;
import org.stockwellness.domain.stock.insight.IndexWeather;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@JdbcTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({TechnicalIndicatorAdapter.class, IndexDailyReadAdapter.class, IndexWeatherAdapter.class,
        SectorLeadingStockAdapter.class})
class TechnicalIndicatorPostgresTest {
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 5);
    private static final Instant CALCULATED_AT = Instant.parse("2026-10-05T08:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        ManualSqlTestSupport.initializeRedesignedPostgres(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private TechnicalIndicatorAdapter adapter;
    @Autowired private IndexDailyReadAdapter indexReadAdapter;
    @Autowired private IndexWeatherAdapter weatherAdapter;
    @Autowired private SectorLeadingStockAdapter leadingStockAdapter;
    @Autowired private JdbcTemplate jdbc;
    private long stockId;
    private long seriesId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM stock_technical_indicator");
        jdbc.update("DELETE FROM index_technical_indicator");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM index_series");
        jdbc.update("DELETE FROM market_index");
        jdbc.update("DELETE FROM stock_price_adjustment");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, status)
                VALUES ('KOSPI', '005930', 'Samsung', 'KRW', 'ACTIVE') RETURNING id
                """, Long.class);
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 100, 102, 99, 101, 1000, 100000, 'FINAL', 'KIS:TEST', now())
                """, stockId, TARGET_DATE);
        long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('TEST:INDEX', 'test index', 'MARKET', 'TST') RETURNING id
                """, Long.class);
        seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.test', 'TST', 'KOSPI', 'PRICE', 'NONE', 'KRW', true) RETURNING id
                """, Long.class, indexId);
        insertIndexDay(TARGET_DATE, "FINAL", true);
        insertIndexDay(TARGET_DATE.minusDays(1), "PROVISIONAL", true);
        insertIndexDay(TARGET_DATE.minusDays(2), "FINAL", false);
    }

    @Test
    void stock_upsert_is_manifest_idempotent_and_updates_only_when_inputs_change() {
        StockTechnicalIndicator value = stockValue("1.010000", "{\"targetDate\":\"2026-10-05\"}", CALCULATED_AT);
        adapter.upsertStock(value);
        java.sql.Timestamp firstCalculatedAt = jdbc.queryForObject(
                "SELECT calculated_at FROM stock_technical_indicator", java.sql.Timestamp.class);
        adapter.upsertStock(stockValue("1.010000", "{\"targetDate\":\"2026-10-05\"}", CALCULATED_AT.plusSeconds(60)));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_technical_indicator", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT calculated_at FROM stock_technical_indicator", java.sql.Timestamp.class))
                .isEqualTo(firstCalculatedAt);

        adapter.upsertStock(stockValue("1.020000", "{\"targetDate\":\"2026-10-05\",\"revision\":2}", CALCULATED_AT.plusSeconds(120)));
        assertThat(jdbc.queryForObject("SELECT ma20_disparity FROM stock_technical_indicator", BigDecimal.class))
                .isEqualByComparingTo("1.020000");
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'revision' FROM stock_technical_indicator", String.class))
                .isEqualTo("2");
    }

    @Test
    void index_upsert_is_idempotent_and_read_port_uses_only_final_complete_rows() {
        adapter.upsertIndex(indexValue("{\"targetDate\":\"2026-10-05\"}"));
        adapter.upsertIndex(indexValue("{\"targetDate\":\"2026-10-05\"}"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM index_technical_indicator", Integer.class)).isEqualTo(1);
        assertThat(indexReadAdapter.recentFinal(seriesId, TARGET_DATE, 10))
                .singleElement().satisfies(value -> {
                    assertThat(value.tradeDate()).isEqualTo(TARGET_DATE);
                    assertThat(value.dataStatus()).isEqualTo("FINAL");
                    assertThat(value.revision()).isEqualTo(1);
                });
    }

    @Test
    void sector_leading_stock_source_is_final_only_and_replacement_is_idempotent() {
        jdbc.update("UPDATE market_index SET kind = 'SECTOR' WHERE id = (SELECT index_id FROM index_series WHERE id = ?)",
                seriesId);
        jdbc.update("UPDATE stock SET sector_index_id = (SELECT index_id FROM index_series WHERE id = ?) WHERE id = ?",
                seriesId, stockId);
        jdbc.update("""
                UPDATE stock_price_daily SET open_price = 109, high_price = 111, low_price = 108,
                    close_price = 110, trading_amount = 120000
                WHERE stock_id = ? AND trade_date = ? AND quote_scope = 'KRX'
                """, stockId, TARGET_DATE);
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 99, 102, 98, 100, 1000, 90000, 'FINAL', 'KIS:TEST', now())
                """, stockId, TARGET_DATE.minusDays(1));

        assertThat(leadingStockAdapter.finalCandidates(seriesId, TARGET_DATE))
                .singleElement().satisfies(candidate -> {
                    assertThat(candidate.previousClose()).isEqualByComparingTo("100");
                    assertThat(candidate.close()).isEqualByComparingTo("110");
                    assertThat(candidate.priceRevision()).isEqualTo(1);
                    assertThat(candidate.indexRevision()).isEqualTo(1);
                });

        List<SectorLeadingStockPort.RankedSectorStock> ranked = List.of(
                new SectorLeadingStockPort.RankedSectorStock(stockId, TARGET_DATE, "KRX", 1));
        leadingStockAdapter.replace(seriesId, TARGET_DATE, ranked, "{\"revision\":1}");
        leadingStockAdapter.replace(seriesId, TARGET_DATE, ranked, "{\"revision\":1}");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sector_leading_stock WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, TARGET_DATE)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM market_signal WHERE signal_type = 'SECTOR_LEADING_STOCK'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'revision' FROM market_signal WHERE signal_type = 'SECTOR_LEADING_STOCK'",
                String.class)).isEqualTo("1");

        leadingStockAdapter.replace(seriesId, TARGET_DATE, List.of(),
                "{\"source\":\"stock_price_daily\",\"indexSeriesId\":" + seriesId
                        + ",\"tradeDate\":\"" + TARGET_DATE + "\",\"candidates\":[],\"selected\":[]}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sector_leading_stock WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, TARGET_DATE)).isZero();
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'stale' FROM market_signal WHERE signal_type = 'SECTOR_LEADING_STOCK'",
                String.class)).isEqualTo("true");
    }

    @Test
    void index_weather_upsert_is_idempotent_and_updates_for_a_new_manifest() {
        weatherAdapter.upsert(weatherValue("55.00", "{\"revision\":1}", CALCULATED_AT));
        java.sql.Timestamp firstCalculatedAt = jdbc.queryForObject(
                "SELECT calculated_at FROM index_weather WHERE series_id = ? AND trade_date = ?",
                java.sql.Timestamp.class, seriesId, TARGET_DATE);
        weatherAdapter.upsert(weatherValue("55.00", "{\"revision\":1}", CALCULATED_AT.plusSeconds(60)));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM index_weather WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, TARGET_DATE)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT calculated_at FROM index_weather WHERE series_id = ? AND trade_date = ?",
                java.sql.Timestamp.class, seriesId, TARGET_DATE)).isEqualTo(firstCalculatedAt);

        weatherAdapter.upsert(weatherValue("60.00", "{\"revision\":2}", CALCULATED_AT.plusSeconds(120)));
        assertThat(jdbc.queryForObject("SELECT score FROM index_weather WHERE series_id = ? AND trade_date = ?",
                BigDecimal.class, seriesId, TARGET_DATE)).isEqualByComparingTo("60.00");
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'revision' FROM index_weather WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, TARGET_DATE)).isEqualTo("2");
    }

    private void insertIndexDay(LocalDate date, String status, boolean complete) {
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, high_value, low_value, close_value,
                    price_status, price_source, collected_at)
                VALUES (?, ?, ?, ?, 100, ?, 'KIS:TEST', now())
                """, seriesId, date, complete ? new BigDecimal("102") : null,
                complete ? new BigDecimal("99") : null, status);
    }

    private StockTechnicalIndicator stockValue(String disparity, String manifest, Instant calculatedAt) {
        return new StockTechnicalIndicator(stockId, TARGET_DATE, "KRX", PriceBasis.RAW,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal(disparity),
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("50"), BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("5"),
                manifest, "ta4j-eod-v1", calculatedAt);
    }

    private IndexTechnicalIndicator indexValue(String manifest) {
        return new IndexTechnicalIndicator(seriesId, TARGET_DATE,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1.000000"),
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("50"), BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("5"),
                manifest, "ta4j-eod-v1", CALCULATED_AT);
    }

    private IndexWeather weatherValue(String score, String manifest, Instant calculatedAt) {
        return new IndexWeather(seriesId, TARGET_DATE, new BigDecimal(score), "FINAL",
                "market-weather-v1", manifest, calculatedAt);
    }
}
