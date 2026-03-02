package org.stockwellness.batch.job.stock.sector.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.batch.job.stock.sector.job.step.DateRangeItemReader;
import org.stockwellness.batch.job.stock.sector.job.step.SectorInsightItemProcessor;
import org.stockwellness.batch.job.stock.sector.job.step.SectorInsightItemWriter;
import org.stockwellness.batch.job.stock.sector.job.listener.SectorEodJobListener;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final DateRangeItemReader dateReader;
    private final SectorInsightItemProcessor processor;
    private final SectorInsightItemWriter writer;
    
    private final SectorEodJobListener jobListener; 

    @Bean
    public Job sectorEodJob() {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(sectorEodStep())
                .listener(jobListener)
                .build();
    }

    @Bean
    public Step sectorEodStep() {
        return new StepBuilder("sectorEodStep", jobRepository)
                .<LocalDate, List<SectorInsight>>chunk(1, transactionManager) // 하루 단위로 처리
                .reader(dateReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }
}
