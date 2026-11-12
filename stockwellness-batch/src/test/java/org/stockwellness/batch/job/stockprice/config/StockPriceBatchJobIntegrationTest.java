package org.stockwellness.batch.job.stockprice.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.adapter.out.persistence.stock.TradingCalendarAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.EodStockPriceRepository;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.BatchIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("StockPriceBatchJob 통합 테스트")
class StockPriceBatchJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dailyStockPriceBatchJob")
    private Job job;

    @Autowired
    private StockAdapter stockAdapter;

    @Autowired
    private TradingCalendarAdapter tradingCalendarAdapter;

    @Autowired
    private EodStockPriceRepository eodStockPriceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("일별 주가 배치는 verified daily source를 target EOD 저장소로 적재한다")
    void stockPriceBatchJob_Success() throws Exception {
        // 준비
        Stock samsung = StockFixture.createSamsung();
        stockAdapter.save(samsung);
        jdbcTemplate.update("update stock set group_code = 'ST' where id = ?", samsung.getId());

        LocalDate targetDate = LocalDate.of(2024, 1, 1);
        tradingCalendarAdapter.upsert(List.of(
                TradingCalendar.of("KOSPI", targetDate, true, null, null, "test")));
        given(kisDailyPriceAdapter.fetchDailyPrices(any(), any(), any()))
                .willReturn(List.of(new KisDailyPriceDetail(targetDate, new BigDecimal("70000"),
                        new BigDecimal("71000"), new BigDecimal("69000"), new BigDecimal("70500"),
                        1_000_000L, new BigDecimal("70500000000"), null, null, "N", null, null, null)));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", "20240101")
                .addString("endDate", "20240101")
                .addString("targetDate", "20240101")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 실행
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .containsExactly("dailyStockPriceStep");

        assertThat(eodStockPriceRepository.count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select close_price from stock_price_daily", BigDecimal.class))
                .isEqualByComparingTo("70500");
        assertThat(jdbcTemplate.queryForObject("select data_status from stock_price_daily", String.class))
                .isEqualTo("PROVISIONAL");
        assertThat(jdbcTemplate.queryForObject("select count(*) from stock_price", Integer.class)).isZero();
    }
}
