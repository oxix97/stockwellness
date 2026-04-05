package org.stockwellness.batch.job.stock.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.batch.listener.JobFailureNotificationListener;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceSyncJobConfig {

    private final KisDailyPriceAdapter kisAdapter;
    private final BenchmarkPricePort benchmarkPricePort;
    private final JobFailureNotificationListener failureNotificationListener;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean
    public Job benchmarkPriceSyncJob(JobRepository jobRepository, Step benchmarkPriceSyncStep) {
        return new JobBuilder("benchmarkPriceSyncJob", jobRepository)
                .listener(failureNotificationListener)
                .start(benchmarkPriceSyncStep)
                .build();
    }

    @Bean
    public Step benchmarkPriceSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BenchmarkPriceDataReader benchmarkPriceDataReader,
            BenchmarkPriceDataProcessor benchmarkPriceDataProcessor,
            BenchmarkPriceDataWriter benchmarkPriceDataWriter
    ) {
        return new StepBuilder("benchmarkPriceSyncStep", jobRepository)
                .<BenchmarkPriceDataWrapper, BenchmarkPrice>chunk(100, transactionManager)
                .reader(benchmarkPriceDataReader)
                .processor(benchmarkPriceDataProcessor)
                .writer(benchmarkPriceDataWriter)
                .build();
    }

    @Bean
    @StepScope
    public BenchmarkPriceDataReader benchmarkPriceDataReader(
            @Value("#{jobParameters['startDate']}") String startDateParam,
            @Value("#{jobParameters['endDate']}") String endDateParam) {

        LocalDate startDate = startDateParam != null ? LocalDate.parse(startDateParam, DATE_FORMATTER) : LocalDate.now().minusYears(2);
        LocalDate endDate = endDateParam != null ? LocalDate.parse(endDateParam, DATE_FORMATTER) : LocalDate.now();

        return new BenchmarkPriceDataReader(kisAdapter, startDate, endDate);
    }

    @Bean
    @StepScope
    public BenchmarkPriceDataProcessor benchmarkPriceProcessor() {
        return new BenchmarkPriceDataProcessor(benchmarkPricePort);
    }

    @Bean
    public BenchmarkPriceDataWriter benchmarkPriceWriter() {
        return new BenchmarkPriceDataWriter(benchmarkPricePort);
    }
}
