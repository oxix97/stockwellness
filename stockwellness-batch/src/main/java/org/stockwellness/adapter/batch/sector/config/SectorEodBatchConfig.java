package org.stockwellness.adapter.batch.sector.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {
    private final JobRepository jobRepository;

    @Bean
    public Job sectorEodJob(@Qualifier("stockTechnicalIndicatorStep") Step stockTechnicalIndicatorStep,
            @Qualifier("indexTechnicalIndicatorStep") Step indexTechnicalIndicatorStep) {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(stockTechnicalIndicatorStep).next(indexTechnicalIndicatorStep).build();
    }
}
