package org.stockwellness.application.port.out;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;

public interface FetchStockPricePort {
    List<Stock> fetchDaily(LocalDate date);
    List<StockHistory> fetchDailyPrice(LocalDate date);
}
