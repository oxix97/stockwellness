package org.stockwellness.batch.job.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.PortfolioStatsRebuildUseCase;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.listener.JobFailureNotificationListener;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PortfolioStatsBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final PortfolioPort portfolioPort;
    private final PortfolioStatsRebuildUseCase portfolioStatsRebuildUseCase;
    private final BatchMdcListener mdcListener;
    private final JobFailureNotificationListener failureNotificationListener;

    private static final int CHUNK_SIZE = 10; // 통계 계산은 헤비하므로 작게 설정

    @Bean
    public Job portfolioStatsJob() {
        return new JobBuilder("portfolioStatsJob", jobRepository)
                .listener(mdcListener)
                .listener(failureNotificationListener)
                .start(portfolioStatsStep())
                .build();
    }

    @Bean
    public Step portfolioStatsStep() {
        return new StepBuilder("portfolioStatsStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, txManager)
                .reader(portfolioItemReader())
                .writer(portfolioStatsWriter())
                .build();
    }

    @Bean
    public ItemReader<Long> portfolioItemReader() {
        return new PortfolioIdPagingReader(portfolioPort, CHUNK_SIZE);
    }

    @Bean
    public PortfolioStatsWriter portfolioStatsWriter() {
        return new PortfolioStatsWriter(portfolioStatsRebuildUseCase);
    }

    @RequiredArgsConstructor
    public static class PortfolioStatsWriter implements ItemWriter<Long> {
        private final PortfolioStatsRebuildUseCase portfolioStatsRebuildUseCase;

        @Override
        public void write(org.springframework.batch.item.Chunk<? extends Long> chunk) {
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
