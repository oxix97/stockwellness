package org.stockwellness.batch.job.stockprice.sync.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.job.stockprice.sync.listener.StockPriceSyncEventListener;

@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;
    private final StockPriceSyncEventListener eventListener;

    @Bean
    public Job stockPriceBatchJob(
            Step stockPriceFetchStep,
            Step stockPriceIndicatorStep
    ) {
        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceFetchStep)
                .next(stockPriceIndicatorStep)
                .listener(commonJobListener)
                .listener(eventListener)
                .build();
    }
}
