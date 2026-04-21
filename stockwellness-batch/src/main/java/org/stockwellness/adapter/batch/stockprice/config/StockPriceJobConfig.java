package org.stockwellness.adapter.batch.stockprice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.adapter.batch.stockprice.listener.StockPriceSyncEventListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceJobConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;
    private final StockPriceSyncEventListener eventListener;

    /**
     * 가격 정보 반영 Job
     */
    @Bean
    public Job dailyStockPriceBatchJob(
            Step dailyStockPriceStep,
            Step technicalIndicatorCalculateStep
    ) {
        return new JobBuilder("dailyStockPriceBatchJob", jobRepository)
                .start(dailyStockPriceStep)
                .next(technicalIndicatorCalculateStep)
                .listener(commonJobListener)
                .listener(eventListener)
                .build();
    }
}
