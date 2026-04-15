package org.stockwellness.application.investortradedetail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.support.BatchMdcListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockInvestorTradeDetailConfig {

    private final JobRepository jobRepository;
    private final BatchMdcListener mdcListener;
    private final JobExecutionListener commonJobListener;

    @Bean
    public Job stockInvestorTradeDetailJob(Step stockInvestorTradeDetailStep) {
        return new JobBuilder("stockInvestorTradeDetailJob", jobRepository)
                .start(stockInvestorTradeDetailStep)
                .listener(mdcListener)
                .listener(commonJobListener)
                .build();
    }
}
