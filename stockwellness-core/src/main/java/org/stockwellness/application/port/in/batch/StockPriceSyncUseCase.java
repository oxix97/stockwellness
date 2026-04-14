package org.stockwellness.application.port.in.batch;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;

public interface StockPriceSyncUseCase {

    StockPriceSyncResult sync(StockPriceBatchCommand command);

    StockPriceSyncResult fetch(StockPriceBatchCommand command);

    StockPriceSyncResult calculateIndicators(StockPriceBatchCommand command);

    record StockPriceBatchCommand(
            List<Stock> stocks,
            String startDate,
            String endDate
    ) {
    }

    record StockPriceSyncResult(List<StockPrice> stockPrices) {
    }
}
