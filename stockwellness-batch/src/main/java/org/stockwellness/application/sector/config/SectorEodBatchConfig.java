package org.stockwellness.application.sector.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.application.sector.listener.SectorEodJobListener;
import org.stockwellness.application.sector.step.processor.SectorAiItemProcessor;
import org.stockwellness.application.sector.step.processor.SectorDailyDetailItemProcessor;
import org.stockwellness.application.sector.step.processor.SectorInsightItemProcessor;
import org.stockwellness.application.sector.step.reader.SectorApiItemReader;
import org.stockwellness.application.sector.step.reader.SectorMarketIndexItemReader;
import org.stockwellness.application.sector.step.writer.SectorAiItemWriter;
import org.stockwellness.application.sector.step.writer.SectorDailyDetailItemWriter;
import org.stockwellness.application.sector.step.writer.SectorInsightItemWriter;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;
import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorEodBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final BatchMdcListener mdcListener;
    private final SectorEodJobListener jobListener;
    private final JobExecutionListener commonJobListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;
    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;

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

    @Bean
    public Step collectSectorDailyDetailStep(
            SectorMarketIndexItemReader reader,
            SectorDailyDetailItemProcessor processor,
            SectorDailyDetailItemWriter writer
    ) {
        return new StepBuilder("collectSectorDailyDetailStep", jobRepository)
                .<MarketIndex, SectorDailyDetail>chunk(15, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

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
                .listener(commonBatchStepLoggingListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean
    public Step sectorAiAnalysisStep(
            JpaPagingItemReader<SectorInsight> sectorReader,
            AsyncItemProcessor<SectorInsight, SectorInsight> asyncProcessor,
            AsyncItemWriter<SectorInsight> asyncWriter
    ) {
        return new StepBuilder("sectorAiAnalysisStep", jobRepository)
                .<SectorInsight, Future<SectorInsight>>chunk(5, transactionManager)
                .reader(sectorReader)
                .processor(asyncProcessor)
                .writer(asyncWriter)
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
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
