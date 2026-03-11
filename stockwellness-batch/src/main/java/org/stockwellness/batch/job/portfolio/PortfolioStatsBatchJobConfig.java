package org.stockwellness.batch.job.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.application.service.portfolio.PortfolioStatBatchService;
import org.stockwellness.batch.common.BatchMdcListener;
import org.stockwellness.domain.portfolio.Portfolio;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PortfolioStatsBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioStatBatchService portfolioStatBatchService;
    private final BatchMdcListener mdcListener;

    private static final int CHUNK_SIZE = 10; // 통계 계산은 헤비하므로 작게 설정

    @Bean
    public Job portfolioStatsJob() {
        return new JobBuilder("portfolioStatsJob", jobRepository)
                .listener(mdcListener)
                .start(portfolioStatsStep())
                .build();
    }

    @Bean
    public Step portfolioStatsStep() {
        return new StepBuilder("portfolioStatsStep", jobRepository)
                .<Portfolio, Portfolio>chunk(CHUNK_SIZE, txManager)
                .reader(portfolioItemReader())
                .writer(portfolioStatsWriter())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Portfolio> portfolioItemReader() {
        return new RepositoryItemReaderBuilder<Portfolio>()
                .name("portfolioItemReader")
                .repository(portfolioRepository)
                .methodName("findAll")
                .pageSize(CHUNK_SIZE)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Portfolio> portfolioStatsWriter() {
        return chunk -> {
            portfolioStatBatchService.updatePortfolioStatsBatch(chunk.getItems());
        };
    }
}
