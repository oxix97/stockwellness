package org.stockwellness.application.port.in.stock;

import org.springframework.data.domain.Slice;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;

import java.util.List;

public interface StockUseCase {
    Slice<StockSearchResult> searchStocks(SearchStockQuery query);
    StockDetailResult getStockDetail(String ticker);
    List<StockSearchResult> getNewListings();
}
