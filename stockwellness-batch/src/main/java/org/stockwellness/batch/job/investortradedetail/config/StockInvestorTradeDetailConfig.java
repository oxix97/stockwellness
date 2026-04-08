package org.stockwellness.batch.job.investortradedetail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;
import org.stockwellness.batch.job.investortradedetail.step.processor.StockInvestorTradeDetailProcessor;
import org.stockwellness.batch.job.investortradedetail.step.reader.StockInvestorTradeDetailReader;
import org.stockwellness.batch.job.investortradedetail.step.writer.StockInvestorTradeDetailWriter;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.listener.JobFailureNotificationListener;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockInvestorTradeDetailConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final KisDailyPriceAdapter kisDailyPriceAdapter;
    private final StockRepository stockRepository;
    private final DataSource dataSource;
    private final BatchMdcListener mdcListener;
    private final JobFailureNotificationListener failureNotificationListener;

    @Bean
    public Job stockInvestorTradeDetailJob(Step stockInvestorTradeDetailStep) {
        return new JobBuilder("stockInvestorTradeDetailJob", jobRepository)
                .start(stockInvestorTradeDetailStep)
                .listener(mdcListener)
                .listener(failureNotificationListener)
                .build();
    }

    @Bean
    public Step stockInvestorTradeDetailStep(
            StockInvestorTradeDetailReader stockInvestorTradeDetailReader,
            StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor,
            StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter
    ) {
        return new StepBuilder("stockInvestorTradeDetailStep", jobRepository)
                .<InvestorTradeDetailUpdateSource, InvestorTradeDetailUpdateCommand>chunk(100, transactionManager)
                .reader(stockInvestorTradeDetailReader)
                .processor(stockInvestorTradeDetailProcessor)
                .writer(stockInvestorTradeDetailWriter)
                .listener(mdcListener)
                .build();
    }

    @Bean
    @StepScope
    public StockInvestorTradeDetailReader stockInvestorTradeDetailReader(
            @Value("#{jobParameters['targetTicker']}") String targetTicker
    ) {
        return new StockInvestorTradeDetailReader(kisDailyPriceAdapter, targetTicker);
    }

    @Bean
    @StepScope
    public StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor(
            @Value("#{jobParameters['baseDate']}") String baseDateParam
    ) {
        LocalDate baseDate = baseDateParam != null && !baseDateParam.isBlank()
                ? DateUtil.parse(baseDateParam)
                : LocalDate.now();
        return new StockInvestorTradeDetailProcessor(stockRepository, baseDate);
    }

    @Bean
    public StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter() {
        return new StockInvestorTradeDetailWriter(dataSource);
    }
}
