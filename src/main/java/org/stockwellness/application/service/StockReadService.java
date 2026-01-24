package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.StockReadUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockReadService implements StockReadUseCase {
    private final LoadStockPort loadStockPort;
    private final LoadStockHistoryPort loadStockHistoryPort;

    @Override
    public Slice<StockSearchResult> searchStocks(SearchStockQuery query) {
        Slice<Stock> stockPage = loadStockPort.searchStocks(query);

        // Entity -> Application Result 변환
        return stockPage.map(stock -> new StockSearchResult(
                stock.getIsinCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                stock.getTotalShares()
        ));
    }

    @Override
    public StockDetailResult getStockDetail(String ticker) {
        // 1. Stock Master 조회
        Stock stock = loadStockPort.loadStockByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다. Ticker: " + ticker));

        // 2. Latest History 조회
        StockHistory h = loadStockHistoryPort.findLatestHistory(stock.getIsinCode())
                .orElse(null);

        // 3. Application Result 매핑
        return new StockDetailResult(
                stock.getIsinCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                null,
                stock.getTotalShares(),
                h != null ? h.getBaseDate() : null,
                h != null ? h.getClosePrice() : BigDecimal.ZERO,
                h != null ? h.getPriceChange() : BigDecimal.ZERO,
                h != null ? h.getFluctuationRate() : BigDecimal.ZERO,
                h != null ? h.getOpenPrice() : BigDecimal.ZERO,
                h != null ? h.getHighPrice() : BigDecimal.ZERO,
                h != null ? h.getLowPrice() : BigDecimal.ZERO,
                h != null ? h.getVolume() : 0L,
                h != null ? h.getTradingValue() : BigDecimal.ZERO,
                h != null ? h.getMarketCap() : BigDecimal.ZERO,
                h != null ? h.getRsi14() : null,
                h != null ? h.getMa20() : null
        );
    }
}
