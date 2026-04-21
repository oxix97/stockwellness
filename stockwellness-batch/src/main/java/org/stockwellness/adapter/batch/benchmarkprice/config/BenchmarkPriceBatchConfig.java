package org.stockwellness.adapter.batch.benchmarkprice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.batch.benchmarkprice.model.BenchmarkPriceDataWrapper;
import org.stockwellness.adapter.batch.benchmarkprice.step.processor.BenchmarkPriceDataProcessor;
import org.stockwellness.adapter.batch.benchmarkprice.step.reader.BenchmarkPriceDataReader;
import org.stockwellness.adapter.batch.benchmarkprice.step.writer.BenchmarkPriceDataWriter;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceBatchConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;
    private final PlatformTransactionManager transactionManager;
    private final KisDailyPriceAdapter kisAdapter;
    private final BenchmarkPriceSyncUseCase benchmarkPriceSyncUseCase;
    private final BenchmarkPricePort benchmarkPricePort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean
    public Job benchmarkPriceSyncJob(Step benchmarkPriceSyncStep) {
        return new JobBuilder("benchmarkPriceSyncJob", jobRepository)
                .listener(commonJobListener)
                .start(benchmarkPriceSyncStep)
                .build();
    }

    @Bean
    public Step benchmarkPriceSyncStep(
            BenchmarkPriceDataReader benchmarkPriceDataReader,
            BenchmarkPriceDataProcessor benchmarkPriceDataProcessor,
            BenchmarkPriceDataWriter benchmarkPriceDataWriter
    ) {
        return new StepBuilder("benchmarkPriceSyncStep", jobRepository)
                .<BenchmarkPriceDataWrapper, BenchmarkPrice>chunk(100, transactionManager)
                .reader(benchmarkPriceDataReader)
                .processor(benchmarkPriceDataProcessor)
                .writer(benchmarkPriceDataWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
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
    public BenchmarkPriceDataProcessor benchmarkPriceDataProcessor() {
        return new BenchmarkPriceDataProcessor(benchmarkPriceSyncUseCase);
    }

    @Bean
    public BenchmarkPriceDataWriter benchmarkPriceDataWriter() {
        return new BenchmarkPriceDataWriter(benchmarkPricePort);
    }
}
