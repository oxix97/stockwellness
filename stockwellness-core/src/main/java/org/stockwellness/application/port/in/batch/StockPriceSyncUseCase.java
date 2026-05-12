package org.stockwellness.application.port.in.batch;

import java.util.List;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.domain.stock.price.StockPrice;

public interface StockPriceSyncUseCase {

    StockPriceSyncResult sync(StockPriceBatchCommand command);

    StockInvestorTradeSyncResult fetchInvestorTrades(StockPriceBatchCommand command);

    StockPriceSyncResult fetch(StockPriceBatchCommand command);

    StockPriceSyncResult calculateIndicators(StockPriceBatchCommand command);

    record StockPriceBatchCommand(
            List<Stock> stocks,
            String startDate,
            String endDate
    ) {
    }

    record StockPriceSyncResult(
            List<StockPrice> stockPrices
    ) {
    }

    record StockInvestorTradeSyncResult(
            List<StockInvestorTrade> investorTrades
    ) {
    }
}
