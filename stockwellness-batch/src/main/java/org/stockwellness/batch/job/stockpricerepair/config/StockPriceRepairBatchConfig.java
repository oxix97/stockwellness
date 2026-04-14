package org.stockwellness.batch.job.stockpricerepair.config;

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
public class StockPriceRepairBatchConfig {

    private final JobRepository jobRepository;
    private final BatchMdcListener mdcListener;
    private final JobExecutionListener commonJobListener;

    @Bean
    public Job stockPricePrevCloseSyncJob(Step stockPricePrevCloseStep) {
        return new JobBuilder("stockPricePrevCloseSyncJob", jobRepository)
                .start(stockPricePrevCloseStep)
                .listener(mdcListener)
                .listener(commonJobListener)
                .build();
    }
}
