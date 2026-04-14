package org.stockwellness.batch.job.stockmaster.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.batch.job.stockmaster.step.processor.StockItemProcessor;
import org.stockwellness.batch.job.stockmaster.step.reader.KosdaqMasterItemReader;
import org.stockwellness.batch.job.stockmaster.step.reader.KospiMasterItemReader;
import org.stockwellness.batch.job.stockmaster.step.tasklet.StockDelistTasklet;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

@Configuration
@RequiredArgsConstructor
public class StockMasterStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final StockMasterSyncUseCase stockMasterSyncUseCase;
    private final BatchMdcListener mdcListener;
    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Step kospiUpsertStep(
            KospiMasterItemReader kospiItemReader,
            StockItemProcessor.Kospi kospiItemProcessor,
            ItemWriter<Stock> stockItemWriter
    ) {
        return new StepBuilder("kospiUpsertStep", jobRepository)
                .<KospiItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kospiItemReader)
                .processor(kospiItemProcessor)
                .writer(stockItemWriter)
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

    @Bean
    public Step kosdaqUpsertStep(
            KosdaqMasterItemReader kosdaqItemReader,
            StockItemProcessor.Kosdaq kosdaqItemProcessor,
            ItemWriter<Stock> stockItemWriter
    ) {
        return new StepBuilder("kosdaqUpsertStep", jobRepository)
                .<KosdaqItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kosdaqItemReader)
                .processor(kosdaqItemProcessor)
                .writer(stockItemWriter)
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
}
