package org.stockwellness.batch.job.stockprice.config;

import java.math.BigDecimal;
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
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.domain.stock.Stock;
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
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("주식 시세 수집 배치가 성공적으로 실행된다 (Gap Sync 흐름)")
    void stockPriceBatchJob_Success() throws Exception {
        // 준비
        Stock samsung = StockFixture.createSamsung();
        stockAdapter.save(samsung);

        KisMultiStockPriceDetail multiPriceDetail = new KisMultiStockPriceDetail(
                "005930",
                "삼성전자",
                new BigDecimal("70500"),
                new BigDecimal("500"),
                new BigDecimal("1.0"),
                new BigDecimal("70000"),
                new BigDecimal("71000"),
                new BigDecimal("69000"),
                1_000_000L,
                new BigDecimal("70500000000"),
                new BigDecimal("70000"),
                BigDecimal.TEN,
                BigDecimal.valueOf(20)
        );
        given(kisDailyPriceAdapter.fetchMultiStockPrices(any()))
                .willReturn(List.of(multiPriceDetail));

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
                .containsExactlyInAnyOrder("dailyStockPriceStep", "technicalIndicatorCalculateStep");

        Integer investorTradeCount = jdbcTemplate.queryForObject("select count(*) from stock_investor_trade", Integer.class);
        assertThat(investorTradeCount).isZero();
    }
}
