package org.stockwellness.application.sector.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.application.sector.listener.SectorEodJobListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;

@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private final JobRepository jobRepository;
    private final SectorEodJobListener jobListener;
    private final JobExecutionListener commonJobListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;

    // MEMO : AI 토큰 없어서 우선 주석처리
    @Bean
    public Job sectorEodJob(
            Step collectSectorDailyDetailStep,
            Step syncSectorInsightStep
    ) {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(collectSectorDailyDetailStep)
                .next(syncSectorInsightStep)
//                .next(sectorAiAnalysisStep)
                .listener(commonJobListener)
                .listener(commonBatchJobLoggingListener)
                .listener(jobListener)
                .build();
    }
}
