package org.stockwellness.application.portfolio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PortfolioStatsBatchStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Step portfolioStatsStep(ItemReader<Long> portfolioItemReader, ItemWriter<Long> portfolioStatsWriter) {
        return new StepBuilder("portfolioStatsStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, txManager)
                .reader(portfolioItemReader)
                .writer(portfolioStatsWriter)
                .build();
    }
}
