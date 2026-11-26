package org.stockwellness.adapter.batch.marketdata.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.StockwellnessBatchApplication;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.adapter.KisHolidayAdapter;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisOverseasIndexDailyPrice;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorTradeByStockDaily;
import org.stockwellness.adapter.out.persistence.stock.TradingCalendarAdapter;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.notification.NotificationPort;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.batch.job.stockmaster.support.StockMasterArchiveFixture;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        classes = StockwellnessBatchApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.batch.jdbc.initialize-schema=never",
                "spring.sql.init.mode=never",
                "spring.batch.job.enabled=false",
                "spring.docker.compose.enabled=false",
                "spring.kafka.admin.auto-create=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.kafka.bootstrap-servers=localhost:9092",
                "management.health.redis.enabled=false",
                "management.health.kafka.enabled=false",
                "KAKAO_CLIENT_ID=dummy-kakao-client-id",
                "KAKAO_CLIENT_SECRET=dummy-kakao-client-secret",
                "GOOGLE_CLIENT_ID=dummy-google-client-id",
                "GOOGLE_CLIENT_SECRET=dummy-google-client-secret",
                "OPENAI_API_KEY=dummy-openai-key",
                "JWT_SECRET=dGhpcy1pcy10ZXN0LXNlY3JldC1rZXktZm9yLXN0b2Nrd2VsbG5lc3MtcHJvamVjdC13aGljaC1pcy1zYWZl",
                "KIS_APP_KEY=dummy-kis-key",
                "KIS_APP_SECRET=dummy-kis-secret",
                "SLACK_WEBHOOK_URL_BATCH="
        })
@ActiveProfiles("prod")
class NewMarketDataBatchConfigIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final Path STAGING_DIRECTORY = createStagingDirectory();

    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 5);
    private static final LocalDate END_DATE = LocalDate.of(2026, 10, 6);

    @Autowired private JobLauncher jobLauncher;
    @Autowired private StockAdapter stockAdapter;
    @Autowired private StockRepository stockRepository;
    @Autowired private MarketIndexRepository marketIndexRepository;
    @Autowired private IndexSeriesRepository indexSeriesRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TradingCalendarAdapter tradingCalendarAdapter;

    @MockitoBean private NotificationPort notificationPort;
    @MockitoBean private RestTemplate restTemplate;
    @MockitoBean private KisMasterClient kisMasterClient;
    @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean private LettuceConnectionFactory redisConnectionFactory;

    @Autowired
    @Qualifier("kisTradingCalendarSyncJob")
    private Job tradingCalendarJob;

    @Autowired
    @Qualifier("kisEodStockPriceSyncJob")
    private Job stockPriceJob;

    @Autowired
    @Qualifier("kisEodIndexPriceSyncJob")
    private Job indexPriceJob;

    @Autowired
    @Qualifier("kisEodInvestorTradeSyncJob")
    private Job investorTradeJob;

    @Autowired
    @Qualifier("marketDerivedDataJob")
    private Job marketDerivedDataJob;

    @Autowired
    @Qualifier("sectorEodJob")
    private Job sectorEodJob;

    @Autowired
    @Qualifier("stockMasterSyncJob")
    private Job stockMasterSyncJob;

    @Autowired
    @Qualifier("dailyStockPriceBatchJob")
    private Job dailyStockPriceBatchJob;

    @Autowired
    @Qualifier("stockInvestorTradeDetailJob")
    private Job stockInvestorTradeDetailJob;

    @MockitoBean private KisDailyPriceAdapter kisDailyPriceAdapter;
    @MockitoBean private KisHolidayAdapter kisHolidayAdapter;
    @MockitoBean private EodStockInvestorTradePort investorTradePort;
    @MockitoBean private MarketDerivedDataUseCase derivedDataUseCase;

    private static Path createStagingDirectory() {
        try {
            return Files.createTempDirectory("stockwellness-canonical-schema-test-").toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void deleteStagingDirectory() throws IOException {
        FileSystemUtils.deleteRecursively(STAGING_DIRECTORY);
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        ManualSqlTestSupport.initializeRedesignedPostgres(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("stockwellness.batch.stock-master.staging-directory", STAGING_DIRECTORY::toString);
    }

    @BeforeEach
    void resetKisPortMocks() {
        org.mockito.Mockito.reset(kisDailyPriceAdapter, kisHolidayAdapter, investorTradePort, derivedDataUseCase);
    }

    @AfterEach
    void removeStockMasterJobFixtures() {
        stockRepository.findByTickerAndMarketCode("910001", "KOSPI").ifPresent(stockRepository::delete);
        stockRepository.findByTickerAndMarketCode("910002", "KOSDAQ").ifPresent(stockRepository::delete);
        jdbcTemplate.update("delete from index_daily where series_id in (select s.id from index_series s join market_index i on i.id = s.index_id where i.index_key in ('KOSPI:0001','KOSDAQ:1001','KOSPI:2001','KOSDAQ:3003','KOSPI:0027'))");
        jdbcTemplate.update("delete from index_series where index_id in (select id from market_index where index_key in ('KOSPI:0001','KOSDAQ:1001','KOSPI:2001','KOSDAQ:3003','KOSPI:0027'))");
        jdbcTemplate.update("delete from market_index where index_key in ('KOSPI:0001','KOSDAQ:1001','KOSPI:2001','KOSDAQ:3003','KOSPI:0027')");
        jdbcTemplate.update("delete from index_daily where series_id in (select s.id from index_series s join market_index i on i.id = s.index_id where i.index_key like 'TEST:KIS:%')");
        jdbcTemplate.update("delete from index_series where index_id in (select id from market_index where index_key like 'TEST:KIS:%')");
        jdbcTemplate.update("delete from market_index where index_key like 'TEST:KIS:%'");
    }

    @AfterEach
    void removeDerivedJobFixtures() {
        jdbcTemplate.update("delete from stock_price_adjustment where stock_id in (select id from stock where ticker in ('005930', '999999', '910001', '910002', '919993', '910004', '910005', '910006', '910007', '910008'))");
        jdbcTemplate.update("delete from stock_price_daily where stock_id in (select id from stock where ticker in ('005930', '999999', '910001', '910002', '919993', '910004', '910005', '910006', '910007', '910008'))");
        jdbcTemplate.update("delete from trading_calendar where source in ('test:kis-252-session', 'test:kis-target-investor', 'test:future', 'test:multiday-incomplete') or trade_date in (date '2026-12-15', date '2026-12-31', date '2027-01-01', date '2027-01-04')");
        jdbcTemplate.update("delete from index_daily where series_id in (select id from index_series where provider = 'KIS' and provider_series_code in ('0001','0027','9099','0001.TR','1001.BAK','1001','SPX'))");
        jdbcTemplate.update("delete from index_series where provider = 'KIS' and provider_series_code in ('0001','0027','9099','0001.TR','1001.BAK','1001','SPX')");
        jdbcTemplate.update("delete from market_index where index_key in ('KOSPI:0001', 'KOSDAQ:1001', 'KOSPI:0027', 'KOSPI:9099', 'SPX')");
        for (String ticker : List.of("005930", "999999", "910001", "910002", "919993", "910004", "910005", "910006", "910007", "910008")) {
            stockRepository.findByTickerAndMarketCode(ticker,
                    ticker.equals("910002") ? "KOSDAQ" : "KOSPI").ifPresent(stockRepository::delete);
        }
    }

    @Test
    void fresh_postgres15_is_initialized_from_manual_sql_and_contains_batch_schema_and_sequences() {
        assertThat(jdbcTemplate.queryForObject("select to_regclass('public.flyway_schema_history') is null", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("select count(*) from market", Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from pg_sequences
                where schemaname = current_schema()
                  and sequencename in ('batch_step_execution_seq', 'batch_job_execution_seq', 'batch_job_seq')
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = current_schema()
                  and table_name in ('batch_job_instance', 'batch_job_execution', 'batch_job_execution_params',
                      'batch_step_execution', 'batch_step_execution_context', 'batch_job_execution_context')
                """, Integer.class)).isEqualTo(6);
    }

    @Test
    void production_batch_context_restarts_and_reads_saved_job_metadata() throws Exception {
        String runId = "restart-" + UUID.randomUUID();
        JobExecution execution = jobLauncher.run(marketDerivedDataJob, new JobParametersBuilder()
                .addString("market", "KR")
                .addString("targetDate", START_DATE.toString())
                .addString("run.id", runId)
                .toJobParameters());
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        try (var restarted = new SpringApplicationBuilder(StockwellnessBatchApplication.class)
                .profiles("prod")
                .run(restartArguments())) {
            JdbcTemplate restartedJdbc = new JdbcTemplate(restarted.getBean(DataSource.class));
            assertThat(restartedJdbc.queryForObject(
                    "select count(*) from batch_job_execution where job_execution_id = ?",
                    Integer.class, execution.getId())).isEqualTo(1);
            assertThat(restartedJdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE'",
                    Integer.class)).isEqualTo(37);
        }
    }

    private static String[] restartArguments() {
        return new String[] {
                "--DB_URL=" + POSTGRES.getJdbcUrl(),
                "--DB_USERNAME=" + POSTGRES.getUsername(),
                "--DB_PASSWORD=" + POSTGRES.getPassword(),
                "--spring.datasource.driver-class-name=" + POSTGRES.getDriverClassName(),
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.sql.init.mode=never",
                "--spring.batch.jdbc.initialize-schema=never",
                "--spring.batch.job.enabled=false",
                "--spring.kafka.listener.auto-startup=false",
                "--spring.kafka.admin.auto-create=false",
                "--spring.kafka.bootstrap-servers=localhost:9092",
                "--spring.cache.type=none",
                "--management.health.redis.enabled=false",
                "--management.health.kafka.enabled=false",
                "--stockwellness.batch.stock-master.staging-directory=" + STAGING_DIRECTORY,
                "--server.port=0",
                "--KAKAO_CLIENT_ID=dummy-kakao-client-id",
                "--KAKAO_CLIENT_SECRET=dummy-kakao-client-secret",
                "--GOOGLE_CLIENT_ID=dummy-google-client-id",
                "--GOOGLE_CLIENT_SECRET=dummy-google-client-secret",
                "--OPENAI_API_KEY=dummy-openai-key",
                "--JWT_SECRET=dGhpcy1pcy10ZXN0LXNlY3JldC1rZXktZm9yLXN0b2Nrd2VsbG5lc3MtcHJvamVjdC13aGljaC1pcy1zYWZl",
                "--KIS_APP_KEY=dummy-kis-key",
                "--KIS_APP_SECRET=dummy-kis-secret",
                "--SLACK_WEBHOOK_URL_BATCH="
        };
    }

    @Test
    void stock_master_job_completes_and_persists_both_markets_on_fresh_postgres15() throws Exception {
        given(kisMasterClient.downloadKospiMasterArchive()).willReturn(StockMasterArchiveFixture.kospi(List.of(
                new StockMasterArchiveFixture.Row("910001", "KR7910001009", "테스트 KOSPI"))));
        given(kisMasterClient.downloadKosdaqMasterArchive()).willReturn(StockMasterArchiveFixture.kosdaq(List.of(
                new StockMasterArchiveFixture.Row("910002", "KR7910002006", "테스트 KOSDAQ"))));

        JobExecution execution = jobLauncher.run(stockMasterSyncJob,
                parameters("testRunId", UUID.randomUUID().toString()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stockRepository.findByTickerAndMarketCode("910001", "KOSPI")).isPresent();
        assertThat(stockRepository.findByTickerAndMarketCode("910002", "KOSDAQ")).isPresent();
    }

    @Test
    void trading_calendar_job_passes_the_base_date_to_the_sync_use_case() throws Exception {
        given(kisHolidayAdapter.fetchHolidays(START_DATE))
                .willReturn(List.of(new KisHolidayEntry(START_DATE.format(DateTimeFormatter.BASIC_ISO_DATE), "Y")));

        JobExecution execution = jobLauncher.run(tradingCalendarJob,
                parameters("baseDate", START_DATE.toString()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("select count(*) from trading_calendar where trade_date = ? and is_trading_day",
                Integer.class, START_DATE)).isEqualTo(2);
        verify(kisHolidayAdapter).fetchHolidays(START_DATE);
    }

    @Test
    void trading_calendar_job_fetches_outside_transaction_and_persists_verified_session_times() throws Exception {
        given(kisHolidayAdapter.fetchHolidays(START_DATE)).willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(new KisHolidayEntry(START_DATE.format(DateTimeFormatter.BASIC_ISO_DATE), "Y"));
        });

        JobExecution execution = jobLauncher.run(tradingCalendarJob,
                parameters("baseDate", START_DATE.toString()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("select count(*) from trading_calendar where trade_date = ? and is_trading_day",
                Integer.class, START_DATE)).isEqualTo(2);
        Map<String, Object> session = jdbcTemplate.queryForMap("""
                select opens_at, closes_at, source from trading_calendar
                where market_code = 'KOSPI' and trade_date = ?
                """, START_DATE);
        assertThat(((java.sql.Timestamp) session.get("opens_at")).toInstant())
                .isEqualTo(Instant.parse("2026-10-05T00:00:00Z"));
        assertThat(((java.sql.Timestamp) session.get("closes_at")).toInstant())
                .isEqualTo(Instant.parse("2026-10-05T06:30:00Z"));
        assertThat(session.get("source")).isEqualTo("KIS:CTCA0903R|KRX:REGULAR");
        verify(kisHolidayAdapter).fetchHolidays(START_DATE);
    }

    @Test
    void trading_calendar_job_skips_unverified_2027_lookahead_while_persisting_2026_dates() throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 12, 15);
        given(kisHolidayAdapter.fetchHolidays(baseDate)).willReturn(List.of(
                new KisHolidayEntry("20261215", "Y"),
                new KisHolidayEntry("20261231", "N"),
                new KisHolidayEntry("20270101", "N"),
                new KisHolidayEntry("20270104", "Y")));

        JobExecution execution = jobLauncher.run(tradingCalendarJob, parameters("baseDate", baseDate.toString()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trading_calendar where trade_date in (date '2026-12-15', date '2026-12-31')",
                Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trading_calendar where trade_date in (date '2027-01-01', date '2027-01-04')",
                Integer.class)).isZero();
    }

    @Test
    void public_stock_and_investor_batch_jobs_reject_future_date_before_calling_kis_or_writing() throws Exception {
        Stock stock = saveDomesticStock("910008", MarketType.KOSPI, StockStatus.ACTIVE, "ST");
        LocalDate futureDate = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(5);
        tradingCalendarAdapter.upsert(List.of(calendarDay("KOSPI", futureDate, true, "test:future")));

        JobExecution canonicalPrice = jobLauncher.run(stockPriceJob,
                dateRangeParameters(futureDate, futureDate, stock.getTicker()));
        JobExecution legacyPrice = jobLauncher.run(dailyStockPriceBatchJob, new JobParametersBuilder()
                .addString("targetTicker", stock.getTicker())
                .addString("startDate", futureDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addString("endDate", futureDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addString("targetDate", futureDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addLong("run.id", System.nanoTime())
                .toJobParameters());
        JobExecution canonicalInvestor = jobLauncher.run(investorTradeJob,
                dateRangeParameters(futureDate, futureDate, stock.getTicker()));
        JobExecution legacyInvestor = jobLauncher.run(stockInvestorTradeDetailJob,
                dateRangeParameters(futureDate, futureDate, stock.getTicker()));

        for (JobExecution execution : List.of(canonicalPrice, legacyPrice, canonicalInvestor, legacyInvestor)) {
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
            assertThat(execution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getExitStatus().getExitDescription()).contains("future"));
        }
        verify(kisDailyPriceAdapter, never()).fetchDailyPrices(any(Stock.class), any(), any());
        verify(kisDailyPriceAdapter, never()).fetchAdjustedDailyPrices(any(Stock.class), any(), any());
        verify(kisDailyPriceAdapter, never()).fetchInvestorTradeByStockDaily(any(Stock.class), any());
        verify(investorTradePort, never()).upsert(any());
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, stock.getId())).isZero();
    }

    @Test
    void public_stock_and_investor_batch_jobs_fail_closed_for_multiday_range_ending_on_incomplete_today_and_succeed_on_retry()
            throws Exception {
        Stock stock = saveDomesticStock("910008", MarketType.KOSPI, StockStatus.ACTIVE, "ST");
        ZoneId korea = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(korea);
        LocalDate yesterday = today.minusDays(1);
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", yesterday, true, "test:multiday-incomplete"),
                TradingCalendar.of("KOSPI", today, true,
                        today.atTime(LocalTime.of(0, 0)).atZone(korea).toOffsetDateTime(),
                        today.atTime(LocalTime.of(23, 59, 59)).atZone(korea).toOffsetDateTime(),
                        "test:multiday-incomplete")));

        JobExecution priceBeforeClose = jobLauncher.run(stockPriceJob,
                dateRangeParameters(yesterday, today, stock.getTicker()));
        JobExecution investorBeforeClose = jobLauncher.run(investorTradeJob,
                dateRangeParameters(yesterday, today, stock.getTicker()));
        JobExecution legacyInvestorBeforeClose = jobLauncher.run(stockInvestorTradeDetailJob,
                dateRangeParameters(yesterday, today, stock.getTicker()));

        for (JobExecution execution : List.of(priceBeforeClose, investorBeforeClose, legacyInvestorBeforeClose)) {
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
            assertThat(execution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getExitStatus().getExitDescription())
                            .contains("No completed KIS"));
        }
        verify(kisDailyPriceAdapter, never()).fetchDailyPrices(any(Stock.class), any(), any());
        verify(kisDailyPriceAdapter, never()).fetchAdjustedDailyPrices(any(Stock.class), any(), any());
        verify(kisDailyPriceAdapter, never()).fetchInvestorTradeByStockDaily(any(Stock.class), any());
        verify(investorTradePort, never()).upsert(any());
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, stock.getId())).isZero();

        tradingCalendarAdapter.upsert(List.of(
                TradingCalendar.of("KOSPI", today, true,
                        today.atTime(LocalTime.of(0, 0)).atZone(korea).toOffsetDateTime(),
                        today.atTime(LocalTime.of(0, 0, 1)).atZone(korea).toOffsetDateTime(),
                        "test:multiday-incomplete")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(yesterday), eq(today)))
                .willReturn(List.of(
                        capturedOrGeneratedRow(yesterday, 0),
                        capturedOrGeneratedRow(today, 1)));
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(any(Stock.class), eq(yesterday), eq(today)))
                .willReturn(List.of(
                        capturedOrGeneratedRow(yesterday, 0),
                        capturedOrGeneratedRow(today, 1)));
        given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(today)))
                .willReturn(List.of(investorRow(yesterday), investorRow(today)));

        JobExecution retriedPrice = jobLauncher.run(stockPriceJob,
                dateRangeParameters(yesterday, today, stock.getTicker()));
        JobExecution retriedInvestor = jobLauncher.run(investorTradeJob,
                dateRangeParameters(yesterday, today, stock.getTicker()));

        assertThat(retriedPrice.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(retriedInvestor.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, stock.getId())).isEqualTo(2);
        verify(investorTradePort).upsert(org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 26));
    }

    @Test
    void stock_eod_job_reread_keeps_atomicity_on_adjusted_failure_and_revises_provisional_without_outbox()
            throws Exception {
        Stock stock = saveDomesticStock("910008", MarketType.KOSPI, StockStatus.ACTIVE, "ST");
        LocalDate tradeDate = LocalDate.of(2026, 11, 12);
        tradingCalendarAdapter.upsert(List.of(calendarDay("KOSPI", tradeDate, true, "test:kis-252-session")));

        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willReturn(List.of(capturedOrGeneratedRow(tradeDate, 0)));
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willReturn(List.of(capturedOrGeneratedRow(tradeDate, 0)));

        JobExecution initial = jobLauncher.run(stockPriceJob, dateRangeParameters(tradeDate, tradeDate, stock.getTicker()));
        assertThat(initial.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        KisDailyPriceDetail failedRawCandidate = new KisDailyPriceDetail(
                tradeDate, new BigDecimal("208500"), new BigDecimal("214000"),
                new BigDecimal("206000"), new BigDecimal("207000"), 19721557L,
                new BigDecimal("4126896638615"), "00", "0.00", "N", "2", new BigDecimal("500"), "");
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willReturn(List.of(failedRawCandidate));
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willThrow(new IllegalStateException("adjusted provider unavailable"));

        JobExecution failedReread = jobLauncher.run(stockPriceJob, dateRangeParameters(tradeDate, tradeDate, stock.getTicker()));
        assertThat(failedReread.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jdbcTemplate.queryForObject(
                "select close_price from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'",
                BigDecimal.class, stock.getId(), tradeDate)).isEqualByComparingTo("206500");
        assertThat(jdbcTemplate.queryForObject(
                "select revision from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'",
                Integer.class, stock.getId(), tradeDate)).isEqualTo(1);

        KisDailyPriceDetail revised208000 = new KisDailyPriceDetail(
                tradeDate, new BigDecimal("208500"), new BigDecimal("214000"),
                new BigDecimal("206000"), new BigDecimal("208000"), 19721557L,
                new BigDecimal("4126896638615"), "00", "0.00", "N", "2", new BigDecimal("500"), "");
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willReturn(List.of(revised208000));
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(any(Stock.class), eq(tradeDate), eq(tradeDate)))
                .willReturn(List.of(revised208000));

        JobExecution revised = jobLauncher.run(stockPriceJob, dateRangeParameters(tradeDate, tradeDate, stock.getTicker()));
        assertThat(revised.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject(
                "select close_price from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'",
                BigDecimal.class, stock.getId(), tradeDate)).isEqualByComparingTo("208000");
        assertThat(jdbcTemplate.queryForObject(
                "select revision from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'",
                Integer.class, stock.getId(), tradeDate)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select data_status from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'",
                String.class, stock.getId(), tradeDate)).isEqualTo("PROVISIONAL");
        assertThat(jdbcTemplate.queryForObject("select count(*) from outbox_event", Integer.class)).isZero();
    }

    @Test
    void stock_eod_job_reads_collectible_domestic_stocks_and_passes_the_date_range() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        setGroupCode(samsung, "ST");
        stockAdapter.save(StockFixture.createDelisted("999999", "상장폐지 종목"));
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", START_DATE, true, "test"),
                calendarDay("KOSPI", END_DATE, true, "test")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(END_DATE)))
                .willReturn(List.of(new KisDailyPriceDetail(START_DATE, new BigDecimal("70000"),
                        new BigDecimal("71000"), new BigDecimal("69000"), new BigDecimal("70500"),
                        1_000_000L, new BigDecimal("70500000000"), null, null, "N", null, null, null),
                        new KisDailyPriceDetail(END_DATE, new BigDecimal("70500"),
                                new BigDecimal("71500"), new BigDecimal("69500"), new BigDecimal("71000"),
                                1_100_000L, new BigDecimal("78100000000"), null, null, "N", null, null, null)));

        JobExecution execution = jobLauncher.run(stockPriceJob, dateRangeParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?", Integer.class,
                samsung.getId())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                select data_status from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'
                """, String.class, samsung.getId(), START_DATE)).isEqualTo("PROVISIONAL");
        assertThat(jdbcTemplate.queryForObject("""
                select data_status from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'
                """, String.class, samsung.getId(), END_DATE)).isEqualTo("PROVISIONAL");
        verify(kisDailyPriceAdapter).fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(END_DATE));
    }

    @Test
    void price_and_investor_jobs_share_the_existing_active_stocks_etfs_and_etns_target_scope() throws Exception {
        List<Stock> fixtures = new ArrayList<>();
        fixtures.add(saveDomesticStock("920011", MarketType.KOSPI, StockStatus.ACTIVE, "ST"));
        fixtures.add(saveDomesticStock("920012", MarketType.KOSDAQ, StockStatus.ACTIVE, "EF"));
        fixtures.add(saveDomesticStock("920013", MarketType.KOSDAQ, StockStatus.ACTIVE, "EN"));
        fixtures.add(saveDomesticStock("0001A0", MarketType.KOSDAQ, StockStatus.ACTIVE, "ST"));
        fixtures.add(saveDomesticStock("920014", MarketType.KOSPI, StockStatus.ACTIVE, "BC"));
        fixtures.add(saveDomesticStock("920015", MarketType.KOSPI, StockStatus.HALTED, "ST"));
        fixtures.add(saveDomesticStock("920016", MarketType.KOSDAQ, StockStatus.ADMINISTRATIVE, "ST"));
        fixtures.add(saveDomesticStock("A92017", MarketType.KOSPI, StockStatus.ACTIVE, "ST"));
        fixtures.add(saveDomesticStock("920019", MarketType.KOSPI, StockStatus.DELISTED, "ST"));

        try {
            tradingCalendarAdapter.upsert(List.of(
                    calendarDay("KOSPI", START_DATE, true, "test:target-universe"),
                    calendarDay("KOSPI", END_DATE, true, "test:target-universe"),
                    calendarDay("KOSDAQ", START_DATE, true, "test:target-universe"),
                    calendarDay("KOSDAQ", END_DATE, true, "test:target-universe")));
            given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(END_DATE)))
                    .willReturn(List.of(
                            new KisDailyPriceDetail(START_DATE, new BigDecimal("70000"), new BigDecimal("71000"),
                                    new BigDecimal("69000"), new BigDecimal("70500"), 1_000_000L,
                                    new BigDecimal("70500000000"), null, null, "N", null, null, null),
                            new KisDailyPriceDetail(END_DATE, new BigDecimal("70500"), new BigDecimal("71500"),
                                    new BigDecimal("69500"), new BigDecimal("71000"), 1_100_000L,
                                    new BigDecimal("78100000000"), null, null, "N", null, null, null)));
            given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(END_DATE)))
                    .willReturn(List.of(investorRow(START_DATE), investorRow(END_DATE)));

            JobExecution priceExecution = jobLauncher.run(stockPriceJob,
                    dateRangeParameters(START_DATE, END_DATE, ""));
            assertThat(priceExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(priceExecution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(4));
            org.mockito.ArgumentCaptor<Stock> priceTargets = org.mockito.ArgumentCaptor.forClass(Stock.class);
            verify(kisDailyPriceAdapter, org.mockito.Mockito.times(4))
                    .fetchDailyPrices(priceTargets.capture(), eq(START_DATE), eq(END_DATE));
            assertThat(priceTargets.getAllValues()).extracting(Stock::getTicker)
                    .containsExactlyInAnyOrder("920011", "920012", "920013", "0001A0");

            JobExecution investorExecution = jobLauncher.run(investorTradeJob,
                    dateRangeParameters(START_DATE, END_DATE, ""));
            assertThat(investorExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(investorExecution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(4));
            org.mockito.ArgumentCaptor<Stock> investorTargets = org.mockito.ArgumentCaptor.forClass(Stock.class);
            verify(kisDailyPriceAdapter, org.mockito.Mockito.times(4))
                    .fetchInvestorTradeByStockDaily(investorTargets.capture(), any(LocalDate.class));
            assertThat(investorTargets.getAllValues()).extracting(Stock::getTicker)
                    .containsExactlyInAnyOrder("920011", "920012", "920013", "0001A0");
        } finally {
            for (Stock fixture : fixtures) {
                jdbcTemplate.update("delete from stock_price_daily where stock_id = ?", fixture.getId());
                stockRepository.delete(fixture);
            }
            jdbcTemplate.update("delete from trading_calendar where source = 'test:target-universe'");
        }
    }

    @Test
    void stock_eod_job_targets_one_ticker_and_reconciles_a_captured_row_during_a_252_session_postgres_load()
            throws Exception {
        Stock selected = stockAdapter.save(Stock.of("910004", "KR7910004002", "대사 대상",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        Stock other = stockAdapter.save(Stock.of("910005", "KR7910005009", "비대상",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        setGroupCode(selected, "ST");
        setGroupCode(other, "ST");
        LocalDate capturedDate = LocalDate.of(2026, 11, 12);
        List<LocalDate> sessions = weekdaySessionsEndingAt(capturedDate, 252);
        LocalDate start = sessions.getFirst();
        Map<LocalDate, Integer> ordinalByDate = new HashMap<>();
        for (int i = 0; i < sessions.size(); i++) ordinalByDate.put(sessions.get(i), i);

        List<TradingCalendar> calendar = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(capturedDate); date = date.plusDays(1)) {
            boolean open = date.getDayOfWeek().getValue() < 6;
            for (String market : List.of("KOSPI", "KOSDAQ")) {
                calendar.add(calendarDay(market, date, open, "test:kis-252-session"));
            }
        }
        tradingCalendarAdapter.upsert(calendar);
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), any(LocalDate.class), any(LocalDate.class)))
                .willAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(1);
                    LocalDate to = invocation.getArgument(2);
                    return sessions.stream().filter(date -> !date.isBefore(from) && !date.isAfter(to))
                            .map(date -> capturedOrGeneratedRow(date, ordinalByDate.get(date))).toList();
                });
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(
                any(Stock.class), any(LocalDate.class), any(LocalDate.class))).willReturn(List.of());

        JobExecution execution = jobLauncher.run(stockPriceJob, dateRangeParameters(start, capturedDate, selected.getTicker()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, selected.getId())).isEqualTo(252);
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, other.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ? and data_status <> 'PROVISIONAL'",
                Integer.class, selected.getId())).isZero();

        Map<String, Object> reconciled = jdbcTemplate.queryForMap("""
                select open_price, high_price, low_price, close_price, volume, trading_amount, data_status, source
                from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'
                """, selected.getId(), capturedDate);
        assertThat((BigDecimal) reconciled.get("open_price")).isEqualByComparingTo("208500");
        assertThat((BigDecimal) reconciled.get("high_price")).isEqualByComparingTo("214000");
        assertThat((BigDecimal) reconciled.get("low_price")).isEqualByComparingTo("206000");
        assertThat((BigDecimal) reconciled.get("close_price")).isEqualByComparingTo("206500");
        assertThat(reconciled.get("volume")).isEqualTo(19721557L);
        assertThat((BigDecimal) reconciled.get("trading_amount")).isEqualByComparingTo("4126896638615");
        assertThat(reconciled.get("data_status")).isEqualTo("PROVISIONAL");
        assertThat(reconciled.get("source")).isEqualTo("KIS:FHKST03010100:RAW");

        JobExecution replay = jobLauncher.run(stockPriceJob, dateRangeParameters(start, capturedDate, selected.getTicker()));
        assertThat(replay.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price_daily where stock_id = ?",
                Integer.class, selected.getId())).isEqualTo(252);
        assertThat(jdbcTemplate.queryForObject("select count(distinct revision) from stock_price_daily where stock_id = ?",
                Integer.class, selected.getId())).isEqualTo(1);
    }

    @Test
    void investor_eod_job_targets_only_the_requested_ticker() throws Exception {
        Stock selected = stockAdapter.save(Stock.of("910006", "KR7910006006", "수급 대상",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        Stock other = stockAdapter.save(Stock.of("910007", "KR7910007003", "비대상 수급",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        setGroupCode(selected, "ST");
        setGroupCode(other, "ST");
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", START_DATE, true, "test:kis-target-investor")));
        given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(START_DATE)))
                .willReturn(List.of(investorRow(START_DATE)));

        JobExecution execution = jobLauncher.run(investorTradeJob,
                dateRangeParameters(START_DATE, START_DATE, selected.getTicker()));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(1));
        verify(kisDailyPriceAdapter).fetchInvestorTradeByStockDaily(any(Stock.class), eq(START_DATE));
        verify(investorTradePort).upsert(org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 13));
    }

    @Test
    void eod_price_and_investor_readers_skip_active_stocks_with_unsupported_group_codes() throws Exception {
        Stock unsupported = stockAdapter.save(Stock.of("910004", "KR7910004002", "범위 제외 종목",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        setGroupCode(unsupported, "ZZ");
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", START_DATE, true, "test:kis-target-investor")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(START_DATE)))
                .willReturn(List.of(capturedOrGeneratedRow(START_DATE, 0)));
        given(kisDailyPriceAdapter.fetchAdjustedDailyPrices(any(Stock.class), eq(START_DATE), eq(START_DATE)))
                .willReturn(List.of());
        given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(START_DATE)))
                .willReturn(List.of(investorRow(START_DATE)));

        JobExecution priceExecution = jobLauncher.run(stockPriceJob,
                dateRangeParameters(START_DATE, START_DATE, unsupported.getTicker()));
        JobExecution investorExecution = jobLauncher.run(investorTradeJob,
                dateRangeParameters(START_DATE, START_DATE, unsupported.getTicker()));

        assertThat(priceExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(priceExecution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isZero());
        assertThat(investorExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(investorExecution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isZero());
        verify(kisDailyPriceAdapter, org.mockito.Mockito.never())
                .fetchDailyPrices(any(Stock.class), any(LocalDate.class), any(LocalDate.class));
        verify(kisDailyPriceAdapter, org.mockito.Mockito.never())
                .fetchAdjustedDailyPrices(any(Stock.class), any(LocalDate.class), any(LocalDate.class));
        verify(kisDailyPriceAdapter, org.mockito.Mockito.never())
                .fetchInvestorTradeByStockDaily(any(Stock.class), any(LocalDate.class));
    }

    @Test
    void stock_eod_job_fails_after_persisting_an_incomplete_open_day_observation() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        setGroupCode(samsung, "ST");
        tradingCalendarAdapter.upsert(List.of(calendarDay("KOSPI", START_DATE, true, "test")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(START_DATE)))
                .willReturn(List.of());

        JobExecution execution = jobLauncher.run(stockPriceJob, singleDateParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jdbcTemplate.queryForObject("""
                select data_status from stock_price_daily where stock_id = ? and trade_date = ? and quote_scope = 'KRX'
                """, String.class, samsung.getId(), START_DATE)).isEqualTo("INCOMPLETE");
    }

    @Test
    void investor_eod_job_saves_provider_rows_without_a_stock_price_dependency() throws Exception {
        Stock stock = stockAdapter.save(Stock.of("910003", "KR7910003003", "수급 테스트",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        setGroupCode(stock, "ST");
        LocalDate secondDate = START_DATE.plusDays(1);
        try {
            tradingCalendarAdapter.upsert(List.of(
                    calendarDay("KOSPI", START_DATE, true, "test"),
                    calendarDay("KOSPI", secondDate, true, "test")));
            given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(secondDate)))
                    .willReturn(List.of(investorRow(START_DATE), investorRow(secondDate)));

            JobExecution execution = jobLauncher.run(investorTradeJob,
                    dateRangeParameters(START_DATE, secondDate, stock.getTicker()));

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(execution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(1));
            assertThat(jdbcTemplate.queryForObject("select to_regclass('public.stock_price') is null", Boolean.class))
                    .isTrue();
            verify(kisDailyPriceAdapter).fetchInvestorTradeByStockDaily(any(Stock.class), eq(secondDate));
            verify(investorTradePort).upsert(org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 26
                    && rows.stream().allMatch(row -> "PROVISIONAL".equals(row.dataStatus()))));
        } finally {
            stockRepository.delete(stock);
        }
    }

    @Test
    void index_eod_job_reads_kis_market_indices_and_passes_the_date_range() throws Exception {
        MarketIndex kospi = marketIndexRepository.saveAndFlush(MarketIndex.of("0001", "코스피", "0"));
        MarketIndex kosdaq = marketIndexRepository.saveAndFlush(MarketIndex.of("1001", "코스닥", "1"));
        MarketIndex kospi200 = marketIndexRepository.saveAndFlush(MarketIndex.of("2001", "코스피200", "2"));
        MarketIndex kosdaq150 = marketIndexRepository.saveAndFlush(MarketIndex.of("3003", "코스닥150", "3"));
        MarketIndex sector = marketIndexRepository.saveAndFlush(MarketIndex.of("0027", "제약", "0"));
        indexSeriesRepository.saveAndFlush(new IndexSeries(kospi, "KIS", "kis.idxcode.KOSPI", "0001",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        indexSeriesRepository.saveAndFlush(new IndexSeries(kospi, "KIS", "kis.total-return", "0001.TR",
                "KOSPI", "TOTAL_RETURN", "GROSS", "KRW", true));
        IndexSeries inactive = new IndexSeries(kosdaq, "KIS", "kis.backup.KOSDAQ", "1001.BAK",
                "KOSDAQ", "PRICE", "NONE", "KRW", true);
        inactive.deactivate();
        indexSeriesRepository.saveAndFlush(inactive);
        indexSeriesRepository.saveAndFlush(new IndexSeries(kosdaq, "KIS", "kis.idxcode.KOSDAQ", "1001",
                "KOSDAQ", "PRICE", "NONE", "KRW", true));
        indexSeriesRepository.saveAndFlush(new IndexSeries(kospi200, "KIS", "kis.idxcode.KOSPI", "2001",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        indexSeriesRepository.saveAndFlush(new IndexSeries(kosdaq150, "KIS", "kis.idxcode.KOSDAQ", "3003",
                "KOSDAQ", "PRICE", "NONE", "KRW", true));
        indexSeriesRepository.saveAndFlush(new IndexSeries(sector, "KIS", "kis.idxcode.KOSPI", "0027",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", START_DATE, true, "test"),
                calendarDay("KOSPI", END_DATE, true, "test"),
                calendarDay("KOSDAQ", START_DATE, true, "test"),
                calendarDay("KOSDAQ", END_DATE, true, "test")));
        given(kisDailyPriceAdapter.fetchRawIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE)))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return List.of(
                            new KisDailySectorDetail(START_DATE.format(DateTimeFormatter.BASIC_ISO_DATE),
                                    "2500.00", "10.00", "2", "0.40", "1000000", "2500000000",
                                    "2490.00", "2510.00", "2480.00"),
                            new KisDailySectorDetail(END_DATE.format(DateTimeFormatter.BASIC_ISO_DATE),
                                    "2510.00", "10.00", "2", "0.40", "1100000", "2761000000",
                                    "2500.00", "2520.00", "2490.00"));
                });

        JobExecution execution = jobLauncher.run(indexPriceJob, dateRangeParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(5));
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?, ?, ?, ?)",
                Integer.class, kospi.getId(), kosdaq.getId(), kospi200.getId(), kosdaq150.getId(), sector.getId())).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?, ?, ?, ?) and d.price_status = 'PROVISIONAL'",
                Integer.class, kospi.getId(), kosdaq.getId(), kospi200.getId(), kosdaq150.getId(), sector.getId())).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?, ?, ?, ?) and d.price_status = 'INCOMPLETE'",
                Integer.class, kospi.getId(), kosdaq.getId(), kospi200.getId(), kosdaq150.getId(), sector.getId())).isZero();
        verify(kisDailyPriceAdapter, org.mockito.Mockito.times(5))
                .fetchRawIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE));
    }

    @Test
    void index_eod_job_collects_only_kis_supported_us_index_series() throws Exception {
        MarketIndex spx = marketIndexRepository.saveAndFlush(
                MarketIndex.ofOverseasBenchmark("TEST:KIS:SPX", "S&P 500", "SPX"));
        MarketIndex unsupported = marketIndexRepository.saveAndFlush(
                MarketIndex.ofOverseasBenchmark("TEST:KIS:COMP", "NASDAQ Composite", "COMP"));
        indexSeriesRepository.saveAndFlush(new IndexSeries(spx, "KIS", "kis.ovrs.idxcode.N", "SPX",
                "NYSE", "PRICE", "NONE", "USD", true));
        indexSeriesRepository.saveAndFlush(new IndexSeries(unsupported, "KIS", "kis.ovrs.idxcode.N", "COMP",
                "NASDAQ", "PRICE", "NONE", "USD", true));
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("NYSE", START_DATE, true, "test"),
                calendarDay("NYSE", END_DATE, true, "test")));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE)))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return List.of(
                            new KisOverseasIndexDailyPrice("20261005", "6750.25", "6730", "6760",
                                    "6700", "12345", "N"),
                            new KisOverseasIndexDailyPrice("20261006", "6760.25", "6740", "6770",
                                    "6710", "12346", "N"));
                });

        JobExecution execution = jobLauncher.run(indexPriceJob, marketDateRangeParameters("US"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isGreaterThanOrEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("""
                select d.price_status from index_daily d join index_series s on s.id = d.series_id
                join market_index i on i.id = s.index_id where i.index_key = 'TEST:KIS:SPX' and d.trade_date = ?
                """, String.class, START_DATE)).isEqualTo("PROVISIONAL");
        assertThat(jdbcTemplate.queryForObject("""
                select d.trading_amount from index_daily d join index_series s on s.id = d.series_id
                join market_index i on i.id = s.index_id where i.index_key = 'TEST:KIS:SPX' and d.trade_date = ?
                """, BigDecimal.class, START_DATE)).isNull();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from index_daily d join index_series s on s.id = d.series_id
                join market_index i on i.id = s.index_id where i.index_key = 'TEST:KIS:COMP'
                """, Integer.class)).isZero();
        verify(kisDailyPriceAdapter).fetchOverseasIndexDailyPrices("SPX", START_DATE, END_DATE);
        org.mockito.Mockito.verify(kisDailyPriceAdapter, org.mockito.Mockito.never())
                .fetchOverseasIndexDailyPrices("COMP", START_DATE, END_DATE);
    }

    @Test
    void us_index_job_does_not_read_domestic_series() throws Exception {
        MarketIndex kospi = marketIndexRepository.saveAndFlush(MarketIndex.of("0001", "코스피", "0"));
        indexSeriesRepository.saveAndFlush(new IndexSeries(kospi, "KIS", "kis.idxcode.KOSPI", "0001",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        tradingCalendarAdapter.upsert(List.of(
                calendarDay("KOSPI", START_DATE, true, "test"),
                calendarDay("KOSDAQ", START_DATE, true, "test")));

        String runId = "us-market-isolation-" + UUID.randomUUID();
        JobExecution execution = jobLauncher.run(indexPriceJob, new JobParametersBuilder()
                .addString("market", "US")
                .addString("startDate", START_DATE.toString())
                .addString("endDate", START_DATE.toString())
                .addString("requestedEndDate", END_DATE.toString())
                .addString("targetDate", START_DATE.toString())
                .addString("sessionBasis", "REGULAR")
                .addString("runId", runId)
                .addLong("run.id", System.nanoTime())
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isZero());
        verify(kisDailyPriceAdapter, org.mockito.Mockito.never())
                .fetchRawIndexDailyPrices(any(), any(), any());
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from batch_job_execution_params
                where job_execution_id = ? and parameter_name in
                    ('market', 'startDate', 'endDate', 'requestedEndDate', 'targetDate', 'sessionBasis', 'runId')
                """, Integer.class, execution.getId())).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from batch_job_execution_params
                where job_execution_id = ? and parameter_name = 'runId' and parameter_value = ?
                """, Integer.class, execution.getId(), runId)).isEqualTo(1);
    }

    @Test
    void derived_data_job_dispatches_only_the_requested_markets_targets_to_the_use_case() throws Exception {
        Stock stock = stockAdapter.save(Stock.of("919993", "KR7919993000", "파생 테스트 종목",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        MarketIndex index = marketIndexRepository.saveAndFlush(MarketIndex.of("9099", "파생 테스트 지수", "0"));
        IndexSeries series = indexSeriesRepository.saveAndFlush(new IndexSeries(index, "KIS", "kis.idxcode.KOSPI", "9099",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        MarketIndex kosdaqMarket = marketIndexRepository.saveAndFlush(MarketIndex.of("1001", "KOSDAQ", "1"));
        IndexSeries kosdaqSeries = indexSeriesRepository.saveAndFlush(new IndexSeries(kosdaqMarket, "KIS",
                "kis.idxcode.KOSDAQ", "1001", "KOSDAQ", "PRICE", "NONE", "KRW", true));
        MarketIndex usIndex = marketIndexRepository.saveAndFlush(MarketIndex.of("SPX", "S&P 500"));
        jdbcTemplate.update("update market_index set kind = 'MARKET' where id = ?", usIndex.getId());
        IndexSeries usSeries = indexSeriesRepository.saveAndFlush(new IndexSeries(usIndex, "KIS", "kis.ovrs.idxcode.N", "SPX",
                "NYSE", "PRICE", "NONE", "USD", true));
        try {
            JobExecution krExecution = jobLauncher.run(marketDerivedDataJob, new JobParametersBuilder()
                    .addString("market", "KR")
                    .addString("targetDate", START_DATE.toString())
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters());

            assertThat(krExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(krExecution.getStepExecutions()).extracting(step -> step.getReadCount())
                    .containsExactly(1L, 2L);
            verify(derivedDataUseCase).calculateStockIndicators(stock.getId(), "KRX", START_DATE);
            verify(derivedDataUseCase).calculateIndexIndicators(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexWeather(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateSectorLeadingStocks(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexIndicators(kosdaqSeries.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexWeather(kosdaqSeries.getId(), START_DATE);
            verify(derivedDataUseCase, never()).calculateSectorLeadingStocks(kosdaqSeries.getId(), START_DATE);
            verify(derivedDataUseCase, never()).calculateIndexIndicators(usSeries.getId(), START_DATE);

            clearInvocations(derivedDataUseCase);
            JobExecution usExecution = jobLauncher.run(marketDerivedDataJob, new JobParametersBuilder()
                    .addString("market", "US")
                    .addString("targetDate", START_DATE.toString())
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters());

            assertThat(usExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(usExecution.getStepExecutions()).extracting(step -> step.getReadCount())
                    .containsExactly(0L, 1L);
            verify(derivedDataUseCase, never()).calculateStockIndicators(anyLong(), any(), any());
            verify(derivedDataUseCase, never()).calculateIndexIndicators(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexIndicators(usSeries.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexWeather(usSeries.getId(), START_DATE);
            verify(derivedDataUseCase, never()).calculateSectorLeadingStocks(usSeries.getId(), START_DATE);
        } finally {
            indexSeriesRepository.delete(usSeries);
            indexSeriesRepository.delete(kosdaqSeries);
            indexSeriesRepository.delete(series);
            marketIndexRepository.delete(usIndex);
            marketIndexRepository.delete(kosdaqMarket);
            marketIndexRepository.delete(index);
            stockRepository.delete(stock);
        }
    }

    @Test
    void sector_eod_job_updates_stock_and_index_derived_data() throws Exception {
        Stock stock = stockAdapter.save(Stock.of("919993", "KR7919993000", "섹터 작업 테스트 종목",
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE));
        MarketIndex index = marketIndexRepository.saveAndFlush(MarketIndex.of("9099", "섹터 작업 테스트 지수", "0"));
        IndexSeries series = indexSeriesRepository.saveAndFlush(new IndexSeries(index, "KIS", "kis.idxcode.KOSPI", "9099",
                "KOSPI", "PRICE", "NONE", "KRW", true));
        try {
            JobExecution execution = jobLauncher.run(sectorEodJob, new JobParametersBuilder()
                    .addString("market", "KR")
                    .addString("targetDate", START_DATE.toString())
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters());

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(execution.getStepExecutions()).extracting(step -> step.getStepName())
                    .containsExactly("stockTechnicalIndicatorStep", "indexTechnicalIndicatorStep");
            verify(derivedDataUseCase).calculateStockIndicators(stock.getId(), "KRX", START_DATE);
            verify(derivedDataUseCase).calculateIndexIndicators(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexWeather(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateSectorLeadingStocks(series.getId(), START_DATE);
        } finally {
            indexSeriesRepository.delete(series);
            marketIndexRepository.delete(index);
            stockRepository.delete(stock);
        }
    }

    private static JobParameters dateRangeParameters() {
        return new JobParametersBuilder()
                .addString("startDate", START_DATE.toString())
                .addString("endDate", END_DATE.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private static JobParameters dateRangeParameters(LocalDate start, LocalDate end, String ticker) {
        return new JobParametersBuilder()
                .addString("ticker", ticker)
                .addString("startDate", start.toString())
                .addString("endDate", end.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private Stock saveDomesticStock(String ticker, MarketType market, StockStatus status, String groupCode) {
        Stock stock = stockAdapter.save(Stock.of(ticker, "KR7" + ticker + "000", ticker,
                market, Currency.KRW, StockSector.empty(), status));
        setGroupCode(stock, groupCode);
        return stock;
    }

    private void setGroupCode(Stock stock, String groupCode) {
        jdbcTemplate.update("update stock set group_code = ? where id = ?", groupCode, stock.getId());
    }

    private static List<LocalDate> weekdaySessionsEndingAt(LocalDate endDate, int count) {
        List<LocalDate> sessions = new ArrayList<>(count);
        for (LocalDate date = endDate; sessions.size() < count; date = date.minusDays(1)) {
            if (date.getDayOfWeek().getValue() < 6) sessions.add(date);
        }
        Collections.reverse(sessions);
        return List.copyOf(sessions);
    }

    private static TradingCalendar calendarDay(String market, LocalDate date, boolean open, String source) {
        if (!open) return TradingCalendar.of(market, date, false, null, null, source);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return TradingCalendar.of(market, date, true,
                date.atTime(LocalTime.of(9, 0)).atZone(zone).toOffsetDateTime(),
                date.atTime(LocalTime.of(15, 30)).atZone(zone).toOffsetDateTime(), source);
    }

    private static KisDailyPriceDetail capturedOrGeneratedRow(LocalDate date, int ordinal) {
        if (date.equals(LocalDate.of(2026, 11, 12))) {
            // Mirrors the sanitized KIS source-contract capture for FHKST03010100.
            return new KisDailyPriceDetail(date, new BigDecimal("208500"), new BigDecimal("214000"),
                    new BigDecimal("206000"), new BigDecimal("206500"), 19721557L,
                    new BigDecimal("4126896638615"), "00", "0.00", "N", "2",
                    new BigDecimal("500"), "");
        }
        BigDecimal close = BigDecimal.valueOf(100_000L + ordinal);
        long volume = 1_000L + ordinal;
        return new KisDailyPriceDetail(date, close, close.add(BigDecimal.TEN), close.subtract(BigDecimal.TEN),
                close, volume, close.multiply(BigDecimal.valueOf(volume)), null, null, "N", null, null, null);
    }

    private static JobParameters marketDateRangeParameters(String market) {
        return new JobParametersBuilder()
                .addString("market", market)
                .addString("startDate", START_DATE.toString())
                .addString("endDate", END_DATE.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private static JobParameters singleDateParameters() {
        return new JobParametersBuilder()
                .addString("startDate", START_DATE.toString())
                .addString("endDate", START_DATE.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private static JobParameters parameters(String name, String value) {
        return new JobParametersBuilder()
                .addString(name, value)
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private static KisInvestorTradeByStockDaily investorRow(LocalDate date) {
        String day = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return new KisInvestorTradeByStockDaily(day, "1", "1", "2", "2", "3", "3", "4", "4",
                "5", "5", "6", "6", "7", "7", "8", "8", "9", "9", "10", "10",
                "11", "11", "12", "12", "13", "13");
    }
}
