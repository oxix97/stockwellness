package org.stockwellness.batch.job.sector.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.batch.job.sector.step.*;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class SectorEodStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchMdcListener mdcListener;
    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;

    @Bean
    public Step collectSectorDailyDetailStep(
            SectorMarketIndexItemReader reader,
            SectorDailyDetailItemProcessor processor,
            SectorDailyDetailItemWriter writer
    ) {
        return new StepBuilder("collectSectorDailyDetailStep", jobRepository)
                .<MarketIndex, SectorDailyDetail>chunk(20, transactionManager)
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
}
