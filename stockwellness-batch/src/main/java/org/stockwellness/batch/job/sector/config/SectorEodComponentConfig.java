package org.stockwellness.batch.job.sector.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.stockwellness.batch.job.sector.step.SectorAiItemProcessor;
import org.stockwellness.batch.job.sector.step.SectorAiItemWriter;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SectorEodComponentConfig {

    private final EntityManagerFactory entityManagerFactory;

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
