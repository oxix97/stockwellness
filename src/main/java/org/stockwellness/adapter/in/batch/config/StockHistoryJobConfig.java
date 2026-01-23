package org.stockwellness.adapter.in.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.in.batch.job.StockPriceReader;
import org.stockwellness.adapter.in.batch.job.StockPriceWriter;
import org.stockwellness.application.port.out.stock.FetchStockPricePort;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StockHistoryJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FetchStockPricePort fetchPort;
    private final StockPriceWriter writer;

    @Bean
    public Job stockHistoryJob() {
        return new JobBuilder("stockHistoryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dailyFetchStep())
                .build();
    }

    @Bean
    public Step dailyFetchStep() {
        return new StepBuilder("dailyFetchStep", jobRepository)
                .<List<StockHistory>, List<StockHistory>>chunk(1,transactionManager)
                .reader(dailyPriceApiReader(null,null))
                .writer(writer)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean
    @StepScope
    public StockPriceReader dailyPriceApiReader(
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate
    ) {
        LocalDate start = (startDate == null || startDate.isBlank())
                ? LocalDate.now().minusDays(2)
                : LocalDate.parse(startDate);

        LocalDate end = (endDate == null || endDate.isBlank())
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(endDate);

        return new StockPriceReader(fetchPort, start, end);
    }
}
