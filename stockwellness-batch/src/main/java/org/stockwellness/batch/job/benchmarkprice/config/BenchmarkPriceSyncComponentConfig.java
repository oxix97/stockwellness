package org.stockwellness.batch.job.benchmarkprice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.batch.job.benchmarkprice.step.processor.BenchmarkPriceDataProcessor;
import org.stockwellness.batch.job.benchmarkprice.step.reader.BenchmarkPriceDataReader;
import org.stockwellness.batch.job.benchmarkprice.step.writer.BenchmarkPriceDataWriter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceSyncComponentConfig {

    private final KisDailyPriceAdapter kisAdapter;
    private final BenchmarkPriceSyncUseCase benchmarkPriceSyncUseCase;
    private final BenchmarkPricePort benchmarkPricePort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
        return new BenchmarkPriceDataProcessor(benchmarkPriceSyncUseCase);
    }

    @Bean
    public BenchmarkPriceDataWriter benchmarkPriceWriter() {
        return new BenchmarkPriceDataWriter(benchmarkPricePort);
    }
}
