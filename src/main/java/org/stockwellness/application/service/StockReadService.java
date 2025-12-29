package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.stock.dto.StockDetailResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockSearchRequest;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.application.port.out.StockHistoryRepository;
import org.stockwellness.application.port.out.StockRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockReadService {
    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public Slice<StockResponse> searchStocks(StockSearchRequest request) {
        Slice<Stock> stockPage = stockRepository.searchByCondition(
                request.keyword(),
                request.marketType(),
                request.status(),
                request.toPageable()
        );

        // Entity -> DTO 변환 (Page map 기능 활용)
        return stockPage.map(StockResponse::from);
    }

    public StockDetailResponse getStockDetail(String ticker) {
        // 1. Stock Master 조회 (없으면 404 예외)
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다. Ticker: " + ticker));

        // 2. Latest History 조회 (ISIN 코드로 조회)
        StockHistory latestHistory = stockHistoryRepository.findTopByIsinCodeOrderByBaseDateDesc(stock.getIsinCode())
                .orElse(null); // 시세 데이터가 없으면 null 처리 (신규 상장 등)

        // 3. 결합 및 DTO 반환
        return StockDetailResponse.of(stock, latestHistory);
    }
}
