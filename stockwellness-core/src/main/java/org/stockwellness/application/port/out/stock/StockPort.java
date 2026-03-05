package org.stockwellness.application.port.out.stock;

import org.springframework.data.domain.Slice;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.domain.stock.Stock;

import java.util.List;
import java.util.Optional;

public interface StockPort {
    /**
     * 특정 업종 코드(mediumCode)에 속하는 모든 활성 종목을 조회합니다.
     */
    List<Stock> findBySectorMediumCode(String mediumCode);

    Optional<Stock> loadStockByTicker(String ticker);
    List<Stock> loadStocksByTickers(List<String> tickers);
    boolean existsByTicker(String ticker);
    Slice<Stock> searchStocks(SearchStockQuery query);
}
