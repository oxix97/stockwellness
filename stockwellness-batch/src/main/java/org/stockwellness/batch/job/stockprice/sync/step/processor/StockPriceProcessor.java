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

    public enum Mode {
        FETCH, INDICATOR
    }

    private final StockPriceSyncUseCase stockPriceSyncUseCase;
    private final Mode mode;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    @Override
    public List<StockPrice> process(List<Stock> stocks) {
        if (mode == Mode.FETCH) {
            return stockPriceSyncUseCase.fetch(
                    new StockPriceSyncUseCase.StockPriceBatchCommand(stocks, startDateStr, endDateStr)
            ).stockPrices();
        } else {
            return stockPriceSyncUseCase.calculateIndicators(
                    new StockPriceSyncUseCase.StockPriceBatchCommand(stocks, startDateStr, endDateStr)
            ).stockPrices();
        }
    }
}
