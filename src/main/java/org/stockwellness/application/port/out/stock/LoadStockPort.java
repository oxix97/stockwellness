package org.stockwellness.application.port.out.stock;

import org.springframework.data.domain.Slice;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.domain.stock.Stock;

import java.util.Optional;

public interface LoadStockPort {
    Optional<Stock> loadStockByIsinCode(String isinCode);
    boolean existsByIsinCode(String isinCode);
    Optional<Stock> loadStockByTicker(String ticker);
    Slice<Stock> searchStocks(SearchStockQuery query);
}
