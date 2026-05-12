package org.stockwellness.batch.job.investortradedetail.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.support.BatchIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StockInvestorTradeDetailJob 통합 테스트")
class StockInvestorTradeDetailBatchJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("stockInvestorTradeDetailJob")
    private Job job;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("stock_price가 없는 targetDate면 bean 생성 예외 없이 validation step에서 실패한다")
    void stockInvestorTradeDetailJob_failsAtValidationStep() throws Exception {
        JobExecution jobExecution = jobLauncher.run(job, jobParameters("20260422"));

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .contains("stockInvestorTradeDetailValidationStep");
        assertThat(jobExecution.getAllFailureExceptions())
                .isNotEmpty()
                .allSatisfy(exception -> {
                    assertThat(exception.getMessage()).contains("requestedDate=2026-04-22");
                    assertThat(exception.getMessage()).doesNotContain("Error creating bean");
                });
    }

    private JobParameters jobParameters(String targetDate) {
        return new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
