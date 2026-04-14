package org.stockwellness.batch.job.stockmaster.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;

/**
 * 종목 마스터 동기화 Spring Batch Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMasterSyncJobConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;

    @Bean
    public Job stockMasterSyncJob(
            Step kospiUpsertStep,
            Step kospiDelistStep,
            Step kosdaqUpsertStep,
            Step kosdaqDelistStep
    ) {
        return new JobBuilder("stockMasterSyncJob", jobRepository)
                .listener(commonJobListener)
                .listener(commonBatchJobLoggingListener)
                .start(kospiUpsertStep)
                .next(kospiDelistStep)
                .next(kosdaqUpsertStep)
                .next(kosdaqDelistStep)
                .build();
    }
}
