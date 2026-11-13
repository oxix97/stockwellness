package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;
import org.stockwellness.application.port.out.stock.StockAiAnalysisPort.StockAiAnalysisRecord;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.analysis.TrendStatus;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;
import org.stockwellness.support.ManualSqlTestSupport;

@Testcontainers(disabledWithoutDocker = true)
class StockAiCanonicalDataPostgresTest {
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 11, 10);
    private static final LocalDate FINAL_DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDate PROVISIONAL_DATE = LocalDate.of(2026, 11, 12);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static StockTechnicalDataAdapter stockData;
    private static StockAiAnalysisAdapter aiResults;
    private static TransactionTemplate transaction;
    private Long stockId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        stockData = new StockTechnicalDataAdapter(new NamedParameterJdbcTemplate(dataSource));
        aiResults = new StockAiAnalysisAdapter(jdbc, new ObjectMapper().findAndRegisterModules());
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM index_weather");
        jdbc.update("DELETE FROM index_technical_indicator");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM index_series");
        jdbc.update("DELETE FROM market_index");
        jdbc.update("DELETE FROM stock_ai_analysis");
        jdbc.update("DELETE FROM stock_technical_indicator");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, status)
                VALUES ('KOSPI', '005930', '삼성전자', 'KRW', 'ACTIVE') RETURNING id
                """, Long.class);
    }

    @Test
    void loadTechnicalContext_uses_latest_final_price_and_manifest_only() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertIndicator(FIRST_DATE, "90", "80", "70", "60", indicatorManifest(FIRST_DATE, 1));
        insertPrice(FINAL_DATE, "110", "FINAL", 3);
        insertIndicator(FINAL_DATE, "100", "90", "80", "70", indicatorManifest(FINAL_DATE, 3));
        insertPrice(PROVISIONAL_DATE, "120", "PROVISIONAL", 1);

        var input = stockData.loadTechnicalContext("005930");

        assertThat(input.stockId()).isEqualTo(stockId);
        assertThat(input.stockName()).isEqualTo("삼성전자");
        assertThat(input.tradeDate()).isEqualTo(FINAL_DATE);
        assertThat(input.priceRevision()).isEqualTo(3);
        assertThat(input.context().priceInfo().closePrice()).isEqualByComparingTo("110");
        assertThat(input.context().priceInfo().fluctuationRate()).isEqualByComparingTo("10.0");
        assertThat(input.inputManifest()).contains("observations", "previous_final", "price_revision");
    }

    @Test
    void loadTechnicalContext_does_not_fall_back_when_latest_final_indicator_is_missing() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertIndicator(FIRST_DATE, "90", "80", "70", "60", indicatorManifest(FIRST_DATE, 1));
        insertPrice(FINAL_DATE, "110", "FINAL", 2);

        assertThatThrownBy(() -> stockData.loadTechnicalContext("005930"))
                .isInstanceOf(StockDataNotFoundException.class);
    }

    @Test
    void loadTechnicalContext_rejects_indicator_when_historical_source_revision_is_stale() {
        insertPrice(FIRST_DATE, "100", "FINAL", 1);
        insertPrice(FINAL_DATE, "110", "FINAL", 1);
        insertIndicator(FINAL_DATE, "100", "90", "80", "70",
                indicatorManifest(FINAL_DATE, 1, FIRST_DATE, 1));
        jdbc.update("UPDATE stock_price_daily SET revision = 2 WHERE stock_id = ? AND trade_date = ?",
                stockId, FIRST_DATE);

        assertThatThrownBy(() -> stockData.loadTechnicalContext("005930"))
                .isInstanceOf(StockDataNotFoundException.class);
    }

    @Test
    void loadTechnicalContext_ignores_final_prices_after_korean_as_of_date() {
        LocalDate asOf = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate latestAvailable = asOf.minusDays(1);
        LocalDate future = asOf.plusDays(1);
        insertPrice(latestAvailable, "100", "FINAL", 1);
        insertIndicator(latestAvailable, "90", "80", "70", "60", indicatorManifest(latestAvailable, 1));
        insertPrice(future, "200", "FINAL", 1);
        insertIndicator(future, "190", "180", "170", "160", indicatorManifest(future, 1));

        var input = stockData.loadTechnicalContext("005930");

        assertThat(input.tradeDate()).isEqualTo(latestAvailable);
        assertThat(input.context().priceInfo().closePrice()).isEqualByComparingTo("100");
    }

    @Test
    void same_final_input_updates_one_ai_record_instead_of_duplicating_it() {
        insertPrice(FINAL_DATE, "110", "FINAL", 3);
        insertIndicator(FINAL_DATE, "100", "90", "80", "70", indicatorManifest(FINAL_DATE, 3));
        var input = stockData.loadTechnicalContext("005930");
        StockAnalysisResult first = result("첫 분석");
        StockAnalysisResult rerun = result("재분석");

        transaction.executeWithoutResult(status -> aiResults.save(record(input, first, Instant.parse("2026-11-11T16:00:00Z"))));
        transaction.executeWithoutResult(status -> aiResults.save(record(input, rerun, "gpt-4.1-mini",
                Instant.parse("2026-11-11T16:05:00Z"))));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_ai_analysis", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT result -> 'report' ->> 'title' FROM stock_ai_analysis", String.class))
                .isEqualTo("재분석");
        assertThat(jdbc.queryForObject("SELECT model_version FROM stock_ai_analysis", String.class)).isEqualTo("gpt-4.1-mini");
        assertThat(jdbc.queryForObject("SELECT status FROM stock_ai_analysis", String.class)).isEqualTo("SUCCEEDED");
    }

    @Test
    void canonical_benchmark_read_excludes_provisional_index_rows() {
        Long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('KOSPI:0001', 'KOSPI', 'MARKET', '0001') RETURNING id
                """, Long.class);
        Long seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.idxcode.KOSPI', '0001', 'KOSPI', 'PRICE', 'NONE', 'KRW', true)
                RETURNING id
                """, Long.class, indexId);
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, high_value, low_value, close_value,
                    price_status, price_source, collected_at)
                VALUES (?, ?, 100, null, null, 105, 'FINAL', 'KIS:test', now()),
                       (?, ?, 110, 120, 100, 115, 'PROVISIONAL', 'KIS:test', now())
                """, seriesId, FINAL_DATE, seriesId, PROVISIONAL_DATE);

        var observations = new IndexDailyReadAdapter(jdbc).recentFinal("0001", PROVISIONAL_DATE, 2);

        assertThat(observations).hasSize(1);
        assertThat(observations.getFirst().tradeDate()).isEqualTo(FINAL_DATE);
        assertThat(observations.getFirst().closePrice()).isEqualByComparingTo("105");
    }

    @Test
    void benchmark_history_uses_previous_final_close_and_keeps_only_final_rows() {
        Long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('KOSPI:history', 'KOSPI history', 'MARKET', '0001') RETURNING id
                """, Long.class);
        Long seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.idxcode.KOSPI', '0001', 'KOSPI', 'PRICE', 'NONE', 'KRW', true)
                RETURNING id
                """, Long.class, indexId);
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, close_value, volume, trading_amount,
                    price_status, price_source, collected_at)
                VALUES (?, ?, 95, 100, 1000, 100000, 'FINAL', 'KIS:test', now()),
                       (?, ?, 100, 105, 1100, 115000, 'FINAL', 'KIS:test', now()),
                       (?, ?, 110, 115, 1200, 138000, 'PROVISIONAL', 'KIS:test', now())
                """, seriesId, FIRST_DATE, seriesId, FINAL_DATE, seriesId, PROVISIONAL_DATE);

        var history = new IndexDailyReadAdapter(jdbc).historyFinal("0001", FINAL_DATE, PROVISIONAL_DATE);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().tradeDate()).isEqualTo(FINAL_DATE);
        assertThat(history.getFirst().openPrice()).isEqualByComparingTo("100");
        assertThat(history.getFirst().previousClosePrice()).isEqualByComparingTo("100");
        assertThat(history.getFirst().volume()).isEqualTo(1100L);
        assertThat(history.getFirst().tradingAmount()).isEqualByComparingTo("115000");
    }

    private void insertPrice(LocalDate date, String close, String status, int revision) {
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, volume, trading_amount, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', ?, ?, ?, ?, 100, 1000, ?, 'KIS:FHKST03010100:RAW', ?, ?)
                """, stockId, date, new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), status, java.sql.Timestamp.from(Instant.now()), revision);
    }

    private void insertIndicator(LocalDate date, String ma5, String ma20, String ma60, String ma120, String manifest) {
        jdbc.update("""
                INSERT INTO stock_technical_indicator(stock_id, trade_date, quote_scope, price_basis, ma5, ma20,
                    ma20_disparity, ma60, ma120, rsi14, macd, macd_signal, adx, plus_di, minus_di,
                    input_manifest, algorithm_version, calculated_at)
                VALUES (?, ?, 'KRX', 'RAW', ?, ?, 100, ?, ?, 55, 1, 0.5, 20, 25, 15, ?::jsonb, 'test-v1', ?)
                """, stockId, date, new BigDecimal(ma5), new BigDecimal(ma20), new BigDecimal(ma60),
                new BigDecimal(ma120), manifest, java.sql.Timestamp.from(Instant.now()));
    }

    private String indicatorManifest(LocalDate date, int revision) {
        return """
                {"source":"stock_price_daily","sourceId":%d,"quoteScope":"KRX","priceBasis":"RAW",
                 "targetDate":"%s","observations":[{"date":"%s","revision":%d}]}
        """.formatted(stockId, date, date, revision);
    }

    private String indicatorManifest(LocalDate targetDate, int targetRevision, LocalDate priorDate, int priorRevision) {
        return """
                {"source":"stock_price_daily","sourceId":%d,"quoteScope":"KRX","priceBasis":"RAW",
                 "targetDate":"%s","observations":[{"date":"%s","revision":%d},
                 {"date":"%s","revision":%d}]}
                """.formatted(stockId, targetDate, priorDate, priorRevision, targetDate, targetRevision);
    }

    private StockAiAnalysisRecord record(
            org.stockwellness.application.port.out.stock.LoadTechnicalDataPort.CanonicalStockAnalysisInput input,
            StockAnalysisResult result,
            Instant generatedAt) {
        return record(input, result, "gpt-4o-mini", generatedAt);
    }

    private StockAiAnalysisRecord record(
            org.stockwellness.application.port.out.stock.LoadTechnicalDataPort.CanonicalStockAnalysisInput input,
            StockAnalysisResult result,
            String modelVersion,
            Instant generatedAt) {
        return new StockAiAnalysisRecord(input.stockId(), input.tradeDate(), input.quoteScope(), "STOCK_TECHNICAL",
                modelVersion, "stock-analysis-v1", input.inputManifest(), result, "SUCCEEDED", generatedAt);
    }

    private static StockAnalysisResult result(String title) {
        return new StockAnalysisResult("005930", TrendStatus.REGULAR,
                new AiReport(InvestmentDecision.BUY, 80, title, List.of("reason"), "analysis"));
    }
}
