package org.stockwellness.batch.job.stockprice.sync.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class StockPriceProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private final StockPriceSyncUseCase stockPriceSyncUseCase;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    @Override
    public List<StockPrice> process(List<Stock> stocks) {
        List<StockPrice> result = stockPriceSyncUseCase.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(stocks, startDateStr, endDateStr)
        ).stockPrices();
        return result.isEmpty() ? null : result;
    }
}
