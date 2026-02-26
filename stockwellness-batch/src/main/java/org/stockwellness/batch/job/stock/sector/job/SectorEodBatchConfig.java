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
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.batch.job.stock.sector.job.step.SectorApiItemReader;
import org.stockwellness.batch.job.stock.sector.job.step.SectorInsightItemProcessor;
import org.stockwellness.batch.job.stock.sector.job.step.SectorInsightItemWriter;
import org.stockwellness.batch.job.stock.sector.job.listener.SectorEodJobListener;
import org.stockwellness.domain.stock.insight.SectorInsight;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private static final int CHUNK_SIZE = 50; // 국내 주요 섹터 수 고려

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final SectorApiItemReader reader;
    private final SectorInsightItemProcessor processor;
    private final SectorInsightItemWriter writer;
    
    private final SectorEodJobListener jobListener; 

    @Bean
    public Job sectorEodJob() {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(sectorEodStep())
                .listener(jobListener) // 배치 성공 시 로컬 이벤트 발행
                .build();
    }

    @Bean
    public Step sectorEodStep() {
        return new StepBuilder("sectorEodStep", jobRepository)
                .<SectorApiDto, SectorInsight>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant() // 장애 허용 설정 
                .retryLimit(3)   // API 네트워크 지연 시 최대 3회 재시도 (권장)
                .retry(Exception.class)
                .build();
    }
}