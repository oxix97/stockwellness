package org.stockwellness.batch.job.stockmaster.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stockmaster.step.processor.StockItemProcessor;
import org.stockwellness.batch.job.stockmaster.step.reader.KosdaqMasterItemReader;
import org.stockwellness.batch.job.stockmaster.step.reader.KospiMasterItemReader;
import org.stockwellness.batch.job.stockmaster.step.tasklet.StockDelistTasklet;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;
import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
import org.stockwellness.batch.support.listener.JobFailureNotificationListener;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

/**
 * 종목 마스터 동기화 Spring Batch Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMasterSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final StockMasterSyncUseCase stockMasterSyncUseCase;
    private final StockPort stockPort;
    private final BatchMdcListener mdcListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;
    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;
    private final JobFailureNotificationListener failureNotificationListener;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Job stockMasterSyncJob() {
        return new JobBuilder("stockMasterSyncJob", jobRepository)
                .listener(mdcListener)
                .listener(commonBatchJobLoggingListener)
                .listener(failureNotificationListener)
                .start(kospiUpsertStep())
                .next(kospiDelistStep())
                .next(kosdaqUpsertStep())
                .next(kosdaqDelistStep())
                .build();
    }

    // ── KOSPI Steps ───────────────────────────────────────────────────────────

    @Bean
    public Step kospiUpsertStep() {
        return new StepBuilder("kospiUpsertStep", jobRepository)
                .<KospiItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kospiItemReader())
                .processor(kospiItemProcessor())
                .writer(stockItemWriter())
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    @Bean
    public Step kospiDelistStep() {
        return new StepBuilder("kospiDelistStep", jobRepository)
                .tasklet(new StockDelistTasklet(stockMasterSyncUseCase, MarketType.KOSPI), txManager)
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .build();
    }

    // ── KOSDAQ Steps ──────────────────────────────────────────────────────────

    @Bean
    public Step kosdaqUpsertStep() {
        return new StepBuilder("kosdaqUpsertStep", jobRepository)
                .<KosdaqItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kosdaqItemReader())
                .processor(kosdaqItemProcessor())
                .writer(stockItemWriter())
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    @Bean
    public Step kosdaqDelistStep() {
        return new StepBuilder("kosdaqDelistStep", jobRepository)
                .tasklet(new StockDelistTasklet(stockMasterSyncUseCase, MarketType.KOSDAQ), txManager)
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .build();
    }

    // ── Readers ───────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public KospiMasterItemReader kospiItemReader() {
        return new KospiMasterItemReader(stockMasterSyncUseCase.loadKospiItems());
    }

    @Bean
    @StepScope
    public KosdaqMasterItemReader kosdaqItemReader() {
        return new KosdaqMasterItemReader(stockMasterSyncUseCase.loadKosdaqItems());
    }

    // ── Processors ────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public StockItemProcessor.Kospi kospiItemProcessor() {
        return new StockItemProcessor.Kospi(stockMasterSyncUseCase);
    }

    @Bean
    @StepScope
    public StockItemProcessor.Kosdaq kosdaqItemProcessor() {
        return new StockItemProcessor.Kosdaq(stockMasterSyncUseCase);
    }

    // ── Writer ────────────────────────────────────────────────────────────────

    @Bean
    public ItemWriter<Stock> stockItemWriter() {
        return chunk -> stockPort.saveAll(new java.util.ArrayList<>(chunk.getItems().stream()
                .filter(java.util.Objects::nonNull)
                .toList()));
    }
}
