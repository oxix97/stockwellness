package org.stockwellness.batch.job.benchmarkprice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceSyncJobConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;

    @Bean
    public Job benchmarkPriceSyncJob(Step benchmarkPriceSyncStep) {
        return new JobBuilder("benchmarkPriceSyncJob", jobRepository)
                .listener(commonJobListener)
                .start(benchmarkPriceSyncStep)
                .build();
    }
}
