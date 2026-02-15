package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockPrice;

import java.time.LocalDate;
import java.util.List;

public interface FetchStockPricePort {
    List<Stock> fetchDaily(LocalDate date);
    List<StockPrice> fetchDailyPrice(LocalDate date);
}
