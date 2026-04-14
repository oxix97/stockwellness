package org.stockwellness.batch.job.portfolio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.application.port.in.batch.PortfolioStatsRebuildUseCase;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.batch.job.portfolio.PortfolioStatsBatchJobConfig;

@Configuration
@RequiredArgsConstructor
public class PortfolioStatsBatchComponentConfig {

    private final PortfolioPort portfolioPort;
    private final PortfolioStatsRebuildUseCase portfolioStatsRebuildUseCase;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public ItemReader<Long> portfolioItemReader() {
        return new PortfolioStatsBatchJobConfig.PortfolioIdPagingReader(portfolioPort, CHUNK_SIZE);
    }

    @Bean
    public PortfolioStatsBatchJobConfig.PortfolioStatsWriter portfolioStatsWriter() {
        return new PortfolioStatsBatchJobConfig.PortfolioStatsWriter(portfolioStatsRebuildUseCase);
    }
}
