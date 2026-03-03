package org.stockwellness.batch.job.stock.sector.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
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
    private final SectorEodJobListener jobListener;

    @Bean
    public Job sectorEodJob(Step sectorEodStep) {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(sectorEodStep)
                .listener(jobListener)
                .build();
    }

    @Bean
    public Step sectorEodStep(
            ItemReader<LocalDate> dateReader,
            ItemProcessor<LocalDate, List<SectorInsight>> processor,
            ItemWriter<List<SectorInsight>> writer
    ) {
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
