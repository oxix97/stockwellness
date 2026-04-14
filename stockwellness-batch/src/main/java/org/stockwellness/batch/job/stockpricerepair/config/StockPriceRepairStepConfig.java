package org.stockwellness.batch.job.stockpricerepair.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.batch.job.stockpricerepair.model.StockPriceRepairDto;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.domain.stock.Stock;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StockPriceRepairStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchMdcListener mdcListener;

    @Bean
    public Step stockPricePrevCloseStep(
            ItemReader<Stock> stockRepairReader,
            ItemProcessor<Stock, List<StockPriceRepairDto>> stockPricePrevCloseProcessor,
            ItemWriter<List<StockPriceRepairDto>> stockPriceSimpleRepairWriter
    ) {
        return new StepBuilder("stockPricePrevCloseStep", jobRepository)
                .<Stock, List<StockPriceRepairDto>>chunk(1, transactionManager)
                .reader(stockRepairReader)
                .processor(stockPricePrevCloseProcessor)
                .writer(stockPriceSimpleRepairWriter)
                .listener(mdcListener)
                .build();
    }
}
