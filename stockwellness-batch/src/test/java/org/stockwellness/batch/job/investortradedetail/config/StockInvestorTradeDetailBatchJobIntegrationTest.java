package org.stockwellness.batch.job.investortradedetail.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.batch.NewMarketDataSyncUseCase;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.support.BatchIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("StockInvestorTradeDetailJob 통합 테스트")
class StockInvestorTradeDetailBatchJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("stockInvestorTradeDetailJob")
    private Job job;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NewMarketDataSyncUseCase marketDataSync;

    @Test
    @DisplayName("기존 job 이름으로 canonical 수급 적재 step을 실행한다")
    void stockInvestorTradeDetailJob_runsCanonicalSyncStep() throws Exception {
        Stock stock = stockRepository.saveAndFlush(Stock.of("005930", "KR7005930003", "Samsung", MarketType.KOSPI,
                Currency.KRW, null, StockStatus.ACTIVE));
        jdbcTemplate.update("update stock set group_code = 'ST' where id = ?", stock.getId());
        LocalDate start = LocalDate.of(2026, 4, 21);
        LocalDate end = LocalDate.of(2026, 4, 22);

        JobExecution jobExecution = jobLauncher.run(job, jobParameters(start, end));

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .contains("kisEodInvestorTradeStep");
        verify(marketDataSync).syncStockInvestorTrades(any(Stock.class), eq(start), eq(end));
    }

    private JobParameters jobParameters(LocalDate start, LocalDate end) {
        return new JobParametersBuilder()
                .addString("startDate", start.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addString("endDate", end.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addString("targetDate", end.format(DateTimeFormatter.BASIC_ISO_DATE))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
