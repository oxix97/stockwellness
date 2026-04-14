package org.stockwellness.batch.job.sector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.job.sector.listener.SectorEodJobListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private final JobRepository jobRepository;
    private final SectorEodJobListener jobListener;
    private final JobExecutionListener commonJobListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;

    @Bean
    public Job sectorEodJob(Step collectSectorDailyDetailStep, Step syncSectorInsightStep, Step sectorAiAnalysisStep) {
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
