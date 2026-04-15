package org.stockwellness.application.stock.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.stock.step.processor.StockItemProcessor;
import org.stockwellness.application.stock.step.reader.KosdaqMasterItemReader;
import org.stockwellness.application.stock.step.reader.KospiMasterItemReader;
import org.stockwellness.domain.stock.Stock;

import java.util.ArrayList;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class StockMasterComponentConfig {

    private final StockMasterSyncUseCase stockMasterSyncUseCase;
    private final StockPort stockPort;

    @Bean
    @StepScope
    public KospiMasterItemReader kospiItemReader() {
        return new KospiMasterItemReader(stockMasterSyncUseCase.loadKospiItems());
    }

    @Bean
    @StepScope
    public KosdaqMasterItemReader kosdaqItemReader() {
        return new KosdaqMasterItemReader(stockMasterSyncUseCase.loadKosdaqItems());
    }

    @Bean
    @StepScope
    public StockItemProcessor.Kospi kospiItemProcessor() {
        return new StockItemProcessor.Kospi(stockMasterSyncUseCase);
    }

    @Bean
    @StepScope
    public StockItemProcessor.Kosdaq kosdaqItemProcessor() {
        return new StockItemProcessor.Kosdaq(stockMasterSyncUseCase);
    }

    @Bean
    public ItemWriter<Stock> stockItemWriter() {
        return chunk -> stockPort.saveAll(new ArrayList<>(chunk.getItems().stream()
                .filter(Objects::nonNull)
                .toList()));
    }
}
