package org.stockwellness.adapter.batch.marketdata.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.stockwellness.application.port.out.notification.NotificationPort;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.batch.job.stockmaster.support.StockMasterArchiveFixture;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
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
                "spring.kafka.admin.auto-create=false"
        })
@ActiveProfiles("test")
class NewMarketDataBatchConfigIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

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
    @Qualifier("stockMasterSyncJob")
    private Job stockMasterSyncJob;

    @MockitoBean private KisDailyPriceAdapter kisDailyPriceAdapter;
    @MockitoBean private KisHolidayAdapter kisHolidayAdapter;
    @MockitoBean private EodStockInvestorTradePort investorTradePort;
    @MockitoBean private MarketDerivedDataUseCase derivedDataUseCase;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        ManualSqlTestSupport.initializeLegacyPostgres(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
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
        jdbcTemplate.update("delete from stock_price_daily where stock_id in (select id from stock where ticker in ('005930', '999999', '910001', '910002', '919993'))");
        jdbcTemplate.update("delete from index_daily where series_id in (select id from index_series where provider = 'KIS' and provider_series_code in ('0001','0027','9099','0001.TR','1001.BAK','1001','SPX'))");
        jdbcTemplate.update("delete from index_series where provider = 'KIS' and provider_series_code in ('0001','0027','9099','0001.TR','1001.BAK','1001','SPX')");
        jdbcTemplate.update("delete from market_index where index_key in ('KOSPI:0001', 'KOSDAQ:1001', 'KOSPI:0027', 'KOSPI:9099', 'SPX')");
        for (String ticker : List.of("005930", "999999", "910001", "910002", "919993")) {
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
    void stock_eod_job_reads_collectible_domestic_stocks_and_passes_the_date_range() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        stockAdapter.save(StockFixture.createDelisted("999999", "상장폐지 종목"));
        tradingCalendarAdapter.upsert(List.of(
                TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSPI", END_DATE, true, null, null, "test")));
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
    void stock_eod_job_fails_after_persisting_an_incomplete_open_day_observation() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        tradingCalendarAdapter.upsert(List.of(TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test")));
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
        try {
            tradingCalendarAdapter.upsert(List.of(
                    TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test")));
            given(kisDailyPriceAdapter.fetchInvestorTradeByStockDaily(any(Stock.class), eq(START_DATE)))
                    .willReturn(List.of(investorRow(START_DATE)));

            JobExecution execution = jobLauncher.run(investorTradeJob,
                    parameters("targetDate", START_DATE.toString()));

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(execution.getStepExecutions()).singleElement()
                    .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(1));
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from stock_price where stock_id = ? and base_date = ?",
                    Integer.class, stock.getId(), START_DATE)).isZero();
            verify(kisDailyPriceAdapter).fetchInvestorTradeByStockDaily(any(Stock.class), eq(START_DATE));
            verify(investorTradePort).upsert(org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 13
                    && rows.stream().allMatch(row -> "PROVISIONAL".equals(row.dataStatus()))));
        } finally {
            stockRepository.delete(stock);
        }
    }

    @Test
    void index_eod_job_reads_kis_market_indices_and_passes_the_date_range() throws Exception {
        MarketIndex kospi = marketIndexRepository.saveAndFlush(MarketIndex.of("0001", "코스피", "0"));
        MarketIndex kosdaq = marketIndexRepository.saveAndFlush(MarketIndex.of("1001", "코스닥", "1"));
        MarketIndex kospi200 = marketIndexRepository.saveAndFlush(MarketIndex.of("2001", "코스피200", "0"));
        MarketIndex kosdaq150 = marketIndexRepository.saveAndFlush(MarketIndex.of("3003", "코스닥150", "1"));
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
                TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSPI", END_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSDAQ", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSDAQ", END_DATE, true, null, null, "test")));
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
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE)))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return List.of(new KisOverseasIndexDailyPrice("20261005", "6750.25", "6730", "6760",
                            "6700", "12345", "N"));
                });

        JobExecution execution = jobLauncher.run(indexPriceJob, marketDateRangeParameters("US"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isGreaterThanOrEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("""
                select d.price_status from index_daily d join index_series s on s.id = d.series_id
                join market_index i on i.id = s.index_id where i.index_key = 'TEST:KIS:SPX'
                """, String.class)).isEqualTo("PROVISIONAL");
        assertThat(jdbcTemplate.queryForObject("""
                select d.trading_amount from index_daily d join index_series s on s.id = d.series_id
                join market_index i on i.id = s.index_id where i.index_key = 'TEST:KIS:SPX'
                """, BigDecimal.class)).isNull();
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
                TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSDAQ", START_DATE, true, null, null, "test")));

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
                    .containsExactly(1L, 1L);
            verify(derivedDataUseCase).calculateStockIndicators(stock.getId(), "KRX", START_DATE);
            verify(derivedDataUseCase).calculateIndexIndicators(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateIndexWeather(series.getId(), START_DATE);
            verify(derivedDataUseCase).calculateSectorLeadingStocks(series.getId(), START_DATE);
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
            indexSeriesRepository.delete(series);
            marketIndexRepository.delete(usIndex);
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
