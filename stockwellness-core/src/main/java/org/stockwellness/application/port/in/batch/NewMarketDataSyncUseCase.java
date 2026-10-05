package org.stockwellness.application.port.in.batch;

import java.time.LocalDate;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.MarketIndex;

public interface NewMarketDataSyncUseCase {
    void syncTradingCalendar(LocalDate baseDate);
    void syncStockPrices(Stock stock, LocalDate startDate, LocalDate endDate);
    void syncIndexPrices(MarketIndex index, LocalDate startDate, LocalDate endDate);
}
