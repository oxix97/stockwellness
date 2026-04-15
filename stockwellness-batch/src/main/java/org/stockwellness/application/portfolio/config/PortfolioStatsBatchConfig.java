package org.stockwellness.application.portfolio.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.PortfolioStatsRebuildUseCase;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.batch.support.BatchMdcListener;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PortfolioStatsBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final BatchMdcListener mdcListener;
    private final JobExecutionListener commonJobListener;
    private final PortfolioPort portfolioPort;
    private final PortfolioStatsRebuildUseCase portfolioStatsRebuildUseCase;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job portfolioStatsJob(Step portfolioStatsStep) {
        return new JobBuilder("portfolioStatsJob", jobRepository)
                .listener(mdcListener)
                .listener(commonJobListener)
                .start(portfolioStatsStep)
                .build();
    }

    @Bean
    public Step portfolioStatsStep(ItemReader<Long> portfolioItemReader, ItemWriter<Long> portfolioStatsWriter) {
        return new StepBuilder("portfolioStatsStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, txManager)
                .reader(portfolioItemReader)
                .writer(portfolioStatsWriter)
                .build();
    }

    @Bean
    public ItemReader<Long> portfolioItemReader() {
        return new PortfolioIdPagingReader(portfolioPort, CHUNK_SIZE);
    }

    @Bean
    public ItemWriter<Long> portfolioStatsWriter() {
        return new PortfolioStatsWriter(portfolioStatsRebuildUseCase);
    }

    @RequiredArgsConstructor
    static class PortfolioStatsWriter implements ItemWriter<Long> {
        private final PortfolioStatsRebuildUseCase portfolioStatsRebuildUseCase;

        @Override
        public void write(Chunk<? extends Long> chunk) {
            portfolioStatsRebuildUseCase.rebuild(
                    new PortfolioStatsRebuildUseCase.PortfolioStatsRebuildCommand(List.copyOf(chunk.getItems()))
            );
        }
    }

    @RequiredArgsConstructor
    static class PortfolioIdPagingReader implements ItemReader<Long> {
        private final PortfolioPort portfolioPort;
        private final int pageSize;

        private int offset;
        private List<Long> currentIds = List.of();
        private int currentIndex;

        @Override
        public Long read() {
            if (currentIndex >= currentIds.size()) {
                currentIds = portfolioPort.findAllIds(offset, pageSize);
                currentIndex = 0;
                offset += currentIds.size();
            }
            if (currentIds.isEmpty()) {
                return null;
            }
            return currentIds.get(currentIndex++);
        }
    }
}
