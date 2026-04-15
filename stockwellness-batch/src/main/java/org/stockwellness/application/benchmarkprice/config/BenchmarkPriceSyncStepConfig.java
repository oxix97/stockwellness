package org.stockwellness.application.benchmarkprice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.benchmarkprice.model.BenchmarkPriceDataWrapper;
import org.stockwellness.application.benchmarkprice.step.processor.BenchmarkPriceDataProcessor;
import org.stockwellness.application.benchmarkprice.step.reader.BenchmarkPriceDataReader;
import org.stockwellness.application.benchmarkprice.step.writer.BenchmarkPriceDataWriter;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceSyncStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

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
                .build();
    }
}
