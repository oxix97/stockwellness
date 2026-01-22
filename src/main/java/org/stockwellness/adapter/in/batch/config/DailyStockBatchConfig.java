package org.stockwellness.adapter.in.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.adapter.in.batch.step.JobTimeListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyStockBatchConfig {
    private final JobRepository jobRepository;
    private final Step calculateStock;
    private final JobTimeListener listener;
    private final Step fetchAndSaveStock;

    @Bean
    public Job dailyStockJob() {
        return new JobBuilder("dailyStockJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fetchAndSaveStock)      // Step 1. 적재
                .next(calculateStock) // Step 2. 계산
                .listener(listener)
                .build();
    }
}
