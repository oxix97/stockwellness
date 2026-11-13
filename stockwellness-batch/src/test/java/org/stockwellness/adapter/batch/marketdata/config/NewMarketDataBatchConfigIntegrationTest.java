package org.stockwellness.adapter.batch.marketdata.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;
import org.stockwellness.adapter.out.persistence.stock.TradingCalendarAdapter;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.out.notification.NotificationPort;
import org.stockwellness.batch.job.stockmaster.support.StockMasterArchiveFixture;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.Stock;
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
    @Qualifier("stockMasterSyncJob")
    private Job stockMasterSyncJob;

    @MockitoBean private KisDailyPriceAdapter kisDailyPriceAdapter;
    @MockitoBean private KisHolidayAdapter kisHolidayAdapter;

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
        org.mockito.Mockito.reset(kisDailyPriceAdapter, kisHolidayAdapter);
    }

    @AfterEach
    void removeStockMasterJobFixtures() {
        stockRepository.findByTickerAndMarketCode("910001", "KOSPI").ifPresent(stockRepository::delete);
        stockRepository.findByTickerAndMarketCode("910002", "KOSDAQ").ifPresent(stockRepository::delete);
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
    void stock_eod_job_reads_collectible_domestic_stocks_and_passes_the_date_range() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        stockAdapter.save(StockFixture.createDelisted("999999", "상장폐지 종목"));
        tradingCalendarAdapter.upsert(List.of(
                TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSPI", END_DATE, true, null, null, "test")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(END_DATE)))
                .willReturn(List.of(new KisDailyPriceDetail(START_DATE, new BigDecimal("70000"),
                        new BigDecimal("71000"), new BigDecimal("69000"), new BigDecimal("70500"),
                        1_000_000L, new BigDecimal("70500000000"), null, null, "N", null, null, null)));

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
                """, String.class, samsung.getId(), END_DATE)).isEqualTo("INCOMPLETE");
        verify(kisDailyPriceAdapter).fetchDailyPrices(any(Stock.class), eq(START_DATE), eq(END_DATE));
    }

    @Test
    void index_eod_job_reads_kis_market_indices_and_passes_the_date_range() throws Exception {
        MarketIndex kospi = marketIndexRepository.saveAndFlush(MarketIndex.of("0001", "코스피", "0"));
        MarketIndex kosdaq = marketIndexRepository.saveAndFlush(MarketIndex.of("1001", "코스닥", "1"));
        marketIndexRepository.saveAndFlush(MarketIndex.of("0027", "제약", "0"));
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
        tradingCalendarAdapter.upsert(List.of(
                TradingCalendar.of("KOSPI", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSPI", END_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSDAQ", START_DATE, true, null, null, "test"),
                TradingCalendar.of("KOSDAQ", END_DATE, true, null, null, "test")));
        given(kisDailyPriceAdapter.fetchRawIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE)))
                .willReturn(List.of(new KisDailySectorDetail(START_DATE.format(DateTimeFormatter.BASIC_ISO_DATE),
                        "2500.00", "10.00", "2", "0.40", "1000000", "2500000000",
                        "2490.00", "2510.00", "2480.00")));

        JobExecution execution = jobLauncher.run(indexPriceJob, dateRangeParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement()
                .satisfies(step -> assertThat(step.getReadCount()).isEqualTo(2));
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?)",
                Integer.class, kospi.getId(), kosdaq.getId())).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?) and d.price_status = 'PROVISIONAL'",
                Integer.class, kospi.getId(), kosdaq.getId())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from index_daily d join index_series s on s.id = d.series_id where s.index_id in (?, ?) and d.price_status = 'INCOMPLETE'",
                Integer.class, kospi.getId(), kosdaq.getId())).isEqualTo(2);
        verify(kisDailyPriceAdapter, org.mockito.Mockito.times(2))
                .fetchRawIndexDailyPrices(any(), eq(START_DATE), eq(END_DATE));
    }

    private static JobParameters dateRangeParameters() {
        return new JobParametersBuilder()
                .addString("startDate", START_DATE.toString())
                .addString("endDate", END_DATE.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }

    private static JobParameters parameters(String name, String value) {
        return new JobParametersBuilder()
                .addString(name, value)
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
    }
}
