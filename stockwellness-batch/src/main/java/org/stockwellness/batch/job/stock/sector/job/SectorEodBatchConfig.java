package org.stockwellness.batch.job.stock.sector.job;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.batch.common.BatchMdcListener;
import org.stockwellness.batch.job.stock.sector.job.listener.SectorEodJobListener;
import org.stockwellness.batch.job.stock.sector.job.step.*;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SectorEodJobListener jobListener;
    private final BatchMdcListener mdcListener;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job sectorEodJob(Step syncSectorInsightStep, Step sectorAiAnalysisStep) {
        return new JobBuilder("sectorEodJob", jobRepository)
                .start(syncSectorInsightStep)
                .next(sectorAiAnalysisStep)
                .listener(mdcListener)
                .listener(jobListener)
                .build();
    }

    /**
     * Step 1: 외부 API/DB 데이터를 바탕으로 SectorInsight 동기화
     */
    @Bean
    public Step syncSectorInsightStep(
            SectorApiItemReader reader,
            SectorInsightItemProcessor processor,
            SectorInsightItemWriter writer
    ) {
        return new StepBuilder("syncSectorInsightStep", jobRepository)
                .<SectorApiDto, SectorInsight>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(mdcListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 2: 저장된 SectorInsight를 바탕으로 비동기 AI 분석 수행
     */
    @Bean
    public Step sectorAiAnalysisStep(
            JpaPagingItemReader<SectorInsight> sectorReader,
            AsyncItemProcessor<SectorInsight, SectorInsight> asyncProcessor,
            AsyncItemWriter<SectorInsight> asyncWriter
    ) {
        return new StepBuilder("sectorAiAnalysisStep", jobRepository)
                .<SectorInsight, java.util.concurrent.Future<SectorInsight>>chunk(5, transactionManager)
                .reader(sectorReader)
                .processor(asyncProcessor)
                .writer(asyncWriter)
                .listener(mdcListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SectorInsight> sectorReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr
    ) {
        LocalDate targetDate = (targetDateStr != null) ? LocalDate.parse(targetDateStr) : LocalDate.now();
        
        return new JpaPagingItemReaderBuilder<SectorInsight>()
                .name("sectorReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM SectorInsight s WHERE s.baseDate = :targetDate")
                .parameterValues(Map.of("targetDate", targetDate))
                .pageSize(10)
                .build();
    }

    @Bean
    public AsyncItemProcessor<SectorInsight, SectorInsight> asyncProcessor(
            SectorAiItemProcessor processor, 
            @Qualifier("batchExecutor") TaskExecutor batchExecutor
    ) {
        AsyncItemProcessor<SectorInsight, SectorInsight> asyncProcessor = new AsyncItemProcessor<>();
        asyncProcessor.setDelegate(processor);
        asyncProcessor.setTaskExecutor(batchExecutor);
        return asyncProcessor;
    }

    @Bean
    public AsyncItemWriter<SectorInsight> asyncWriter(SectorAiItemWriter writer) {
        AsyncItemWriter<SectorInsight> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer);
        return asyncWriter;
    }
}
