package org.stockwellness.adapter.batch.investortradedetail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.service.batch.StockInvestorTradeDetailBatchService;
import org.stockwellness.adapter.batch.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.adapter.batch.investortradedetail.step.processor.StockInvestorTradeDetailProcessor;
import org.stockwellness.adapter.batch.investortradedetail.step.reader.StockInvestorTradeDetailReader;
import org.stockwellness.adapter.batch.investortradedetail.step.writer.StockInvestorTradeDetailWriter;
import org.stockwellness.batch.support.BatchMdcListener;

import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockInvestorTradeDetailBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchMdcListener mdcListener;
    private final JobExecutionListener commonJobListener;
    private final StockInvestorTradeDetailBatchService batchService;
    private final StockRepository stockRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Job stockInvestorTradeDetailJob(Step stockInvestorTradeDetailStep) {
        return new JobBuilder("stockInvestorTradeDetailJob", jobRepository)
                .start(stockInvestorTradeDetailStep)
                .listener(mdcListener)
                .listener(commonJobListener)
                .build();
    }

    @Bean
    public Step stockInvestorTradeDetailStep(
            StockInvestorTradeDetailReader stockInvestorTradeDetailReader,
            StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor,
            StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter
    ) {
        return new StepBuilder("stockInvestorTradeDetailStep", jobRepository)
                .<InvestorTradeDetail, InvestorTradeDetailUpdateCommand>chunk(50, transactionManager)
                .reader(stockInvestorTradeDetailReader)
                .processor(stockInvestorTradeDetailProcessor)
                .writer(stockInvestorTradeDetailWriter)
                .listener(mdcListener)
                .build();
    }

    @Bean
    @StepScope
    public StockInvestorTradeDetailReader stockInvestorTradeDetailReader() {
        return new StockInvestorTradeDetailReader(batchService.fetchMergedDetails());
    }

    @Bean
    @StepScope
    public StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor() {
        LocalDate marketBaseDate = batchService.resolveMarketBaseDate();
        return new StockInvestorTradeDetailProcessor(stockRepository, marketBaseDate);
    }

    @Bean
    public StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter() {
        return new StockInvestorTradeDetailWriter(jdbcTemplate);
    }
}
