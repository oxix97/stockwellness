package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.out.StockRepository;
import org.stockwellness.domain.stock.StockHistory;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StockIndicatorCalculate {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StockRepository stockRepository;
    private final StockHistoryProcessor processor;
    private final StockHistoryWriter writer;

    @Bean
    public Step calculateStock() {
        return new StepBuilder("calculateStock", jobRepository)
                .<String, List<StockHistory>>chunk(5, transactionManager)
                .reader(isinCodeReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<String> isinCodeReader() {
        return new ListItemReader<>(stockRepository.findAllStockCodes());
    }

}
