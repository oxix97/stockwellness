package org.stockwellness.application.port.in.stock;

import java.util.List;

import org.springframework.data.domain.Slice;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;

public interface StockUseCase {
    Slice<StockSearchResult> searchStocks(SearchStockQuery query);
    StockDetailResult getStockDetail(String ticker);
    List<StockSearchResult> getNewListings();
}
