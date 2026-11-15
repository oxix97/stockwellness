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
import org.stockwellness.support.ManualSqlTestSupport;

@Testcontainers(disabledWithoutDocker = true)
class SectorMarketReadAdapterPostgresTest {
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 11, 10);
    private static final LocalDate FINAL_DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDate PROVISIONAL_DATE = LocalDate.of(2026, 11, 12);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static SectorMarketReadAdapter sectors;
    private Long seriesId;
    private Long stockId;

    @BeforeAll
    static void initializeSchema() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        sectors = new SectorMarketReadAdapter(jdbc);
    }

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM sector_leading_stock");
        jdbc.update("DELETE FROM market_signal");
        jdbc.update("DELETE FROM index_technical_indicator");
        jdbc.update("DELETE FROM index_daily");
        jdbc.update("DELETE FROM index_series");
        jdbc.update("DELETE FROM stock_price_daily");
        jdbc.update("DELETE FROM stock");
        jdbc.update("DELETE FROM market_index WHERE index_key = 'KOSPI:SEMI' OR kind = 'SECTOR'");
        Long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind) VALUES ('KOSPI:SEMI', '반도체', 'SECTOR') RETURNING id
                """, Long.class);
        seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.sector', 'SEMI', 'KOSPI', 'PRICE', 'NONE', 'KRW', true) RETURNING id
                """, Long.class, indexId);
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, currency, sector_index_id, status)
                VALUES ('KOSPI', '005930', '삼성전자', 'KRW', ?, 'ACTIVE') RETURNING id
                """, Long.class, indexId);
        insertIndexPrice(FIRST_DATE, "100", "FINAL", 1);
        insertIndexPrice(FINAL_DATE, "110", "FINAL", 1);
        insertIndexPrice(PROVISIONAL_DATE, "120", "PROVISIONAL", 1);
        insertStockPrice(FIRST_DATE, "100");
        insertStockPrice(FINAL_DATE, "105");
        jdbc.update("""
                INSERT INTO index_technical_indicator(series_id, trade_date, ma5, ma20, rsi14,
                    input_manifest, algorithm_version, calculated_at)
                VALUES (?, ?, 108, 100, 55, ?::jsonb, 'test-v1', ?)
                """, seriesId, FINAL_DATE, indicatorManifest(), java.sql.Timestamp.from(Instant.now()));
        jdbc.update("""
                INSERT INTO sector_leading_stock(series_id, trade_date, rank, stock_id, price_date, quote_scope)
                VALUES (?, ?, 1, ?, ?, 'KRX')
                """, seriesId, FINAL_DATE, stockId, FINAL_DATE);
        jdbc.update("""
                INSERT INTO market_signal(stock_id, trade_date, quote_scope, signal_type, rule_version,
                                          input_manifest, created_at)
                VALUES (?, ?, 'KRX', 'SECTOR_LEADING_STOCK', 'sector-leading-turnover-gainers-v1', ?::jsonb, now())
                """, stockId, FINAL_DATE, leaderManifest());
    }

    @Test
    void sector_reads_use_final_series_current_indicators_and_final_leading_prices() {
        var ranking = sectors.finalOnDate(FINAL_DATE, "KOSPI");
        var latest = sectors.latestFinal("SEMI", PROVISIONAL_DATE).orElseThrow();
        var history = sectors.historyFinal(seriesId, PROVISIONAL_DATE, 10);
        var leaders = sectors.leadingStocks(seriesId, FINAL_DATE);

        assertThat(sectors.latestFinalDate(null)).contains(FINAL_DATE);
        assertThat(ranking).hasSize(1);
        assertThat(ranking.getFirst().changeRate()).isEqualByComparingTo("10.00000000");
        assertThat(ranking.getFirst().ma20()).isEqualByComparingTo("100");
        assertThat(latest.tradeDate()).isEqualTo(FINAL_DATE);
        assertThat(history).hasSize(2);
        assertThat(leaders).hasSize(1);
        assertThat(leaders.getFirst().fluctuationRate()).isEqualByComparingTo("5.00000000");
    }

    @Test
    void sector_indicator_is_hidden_when_any_source_revision_is_stale() {
        jdbc.update("UPDATE index_daily SET revision = 2 WHERE series_id = ? AND trade_date = ?",
                seriesId, FIRST_DATE);

        var row = sectors.finalOnDate(FINAL_DATE, "KOSPI").getFirst();

        assertThat(row.ma20()).isNull();
        assertThat(row.changeRate()).isEqualByComparingTo("10.00000000");
    }

    @Test
    void sector_leaders_are_hidden_when_any_ranking_input_revision_is_stale() {
        jdbc.update("UPDATE stock_price_daily SET revision = 2 WHERE stock_id = ? AND trade_date = ?",
                stockId, FINAL_DATE);

        assertThat(sectors.leadingStocks(seriesId, FINAL_DATE)).isEmpty();
    }

    @Test
    void sector_reads_ignore_canonical_series_whose_currency_disagrees_with_its_calendar_market() {
        Long mismatchedSeriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                SELECT index_id, 'OTHER', 'test.sector', 'SEMI-USD', 'KOSPI', 'PRICE', 'NONE', 'USD', true
                FROM index_series WHERE id = ? RETURNING id
                """, Long.class, seriesId);
        insertIndexPrice(mismatchedSeriesId, FINAL_DATE, "110");
        jdbc.update("""
                INSERT INTO sector_leading_stock(series_id, trade_date, rank, stock_id, price_date, quote_scope)
                VALUES (?, ?, 1, ?, ?, 'KRX')
                """, mismatchedSeriesId, FINAL_DATE, stockId, FINAL_DATE);

        assertThat(sectors.finalOnDate(FINAL_DATE, "KOSPI")).hasSize(1);
        assertThat(sectors.latestFinalDate(null)).contains(FINAL_DATE);
        assertThat(sectors.leadingStocks(mismatchedSeriesId, FINAL_DATE)).isEmpty();
    }

    private void insertIndexPrice(LocalDate date, String close, String status, int revision) {
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, high_value, low_value, close_value,
                    price_status, price_source, collected_at, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'KIS:test', ?, ?)
                """, seriesId, date, new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), status,
                java.sql.Timestamp.from(Instant.now()), revision);
    }

    private void insertIndexPrice(Long targetSeriesId, LocalDate date, String close) {
        jdbc.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, high_value, low_value, close_value,
                    price_status, price_source, collected_at, revision)
                VALUES (?, ?, ?, ?, ?, ?, 'FINAL', 'KIS:test', ?, 1)
                """, targetSeriesId, date, new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), java.sql.Timestamp.from(Instant.now()));
    }

    private void insertStockPrice(LocalDate date, String close) {
        jdbc.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price, low_price,
                    close_price, volume, trading_amount, data_status, source, collected_at, revision)
                VALUES (?, ?, 'KRX', ?, ?, ?, ?, 100, 10000, 'FINAL', 'KIS:test', ?, 1)
                """, stockId, date, new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), java.sql.Timestamp.from(Instant.now()));
    }

    private String indicatorManifest() {
        return """
                {"source":"index_daily","sourceId":%d,"targetDate":"%s",
                 "observations":[{"date":"%s","revision":1},{"date":"%s","revision":1}]}
                """.formatted(seriesId, FINAL_DATE, FIRST_DATE, FINAL_DATE);
    }

    private String leaderManifest() {
        return """
                {"source":"stock_price_daily","indexSeriesId":%d,"tradeDate":"%s",
                 "candidates":[{"stockId":%d,"priceDate":"%s","quoteScope":"KRX",
                 "priceRevision":1,"indexRevision":1}]}
                """.formatted(seriesId, FINAL_DATE, stockId, FINAL_DATE);
    }
}
