package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.event.StockSearchEvent;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StockService implements StockUseCase {

    private final StockRepository stockRepository;
    private final StockPricePort stockPricePort; // 추가
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Slice<StockSearchResult> searchStocks(SearchStockQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());
        
        // 검색 수행
        var result = stockRepository.searchByCondition(
                query.keyword(),
                query.marketType(),
                query.status(),
                pageable
        );

        // 검색 이벤트 발행 (인기 검색어 집계용)
        if (query.keyword() != null && !query.keyword().isBlank()) {
            eventPublisher.publishEvent(StockSearchEvent.of(query.keyword(), null));
        }

        return result.map(s -> new StockSearchResult(
                s.getTicker(),
                s.getName(),
                s.getMarketType(),
                s.getSector().getSectorName(),
                s.getStatus()
        ));
    }

    @Override
    public StockDetailResult getStockDetail(String ticker) {
        var stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + ticker));

        // 최신 시세 정보 조회
        LocalDate today = LocalDate.now();
        StockPrice latestPrice = stockPricePort.findLatestByTicker(ticker)
                .orElse(null);

        BigDecimal closePrice = (latestPrice != null) ? latestPrice.getClosePrice() : BigDecimal.ZERO;
        BigDecimal prevClose = (latestPrice != null && latestPrice.getPreviousClosePrice() != null) 
                ? latestPrice.getPreviousClosePrice() : closePrice;
        BigDecimal priceChange = closePrice.subtract(prevClose);
        BigDecimal fluctuationRate = prevClose.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                priceChange.divide(prevClose, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new StockDetailResult(
                stock.getStandardCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getSector().getSectorName(),
                stock.getMarketType().name(),
                stock.getParValue(), // totalShares 대용
                (latestPrice != null) ? latestPrice.getId().getBaseDate() : today,
                closePrice,
                priceChange,
                fluctuationRate,
                (latestPrice != null) ? latestPrice.getOpenPrice() : BigDecimal.ZERO,
                (latestPrice != null) ? latestPrice.getHighPrice() : BigDecimal.ZERO,
                (latestPrice != null) ? latestPrice.getLowPrice() : BigDecimal.ZERO,
                (latestPrice != null) ? latestPrice.getVolume() : 0L,
                (latestPrice != null) ? latestPrice.getTransactionAmt() : BigDecimal.ZERO,
                BigDecimal.ZERO, // marketCap (필요 시 추가 계산)
                (latestPrice != null && latestPrice.getIndicators() != null) ? latestPrice.getIndicators().getRsi14() : null,
                (latestPrice != null && latestPrice.getIndicators() != null) ? latestPrice.getIndicators().getMa20() : null,
                (latestPrice != null && latestPrice.getIndicators() != null) ? latestPrice.getIndicators().getAiInsight() : "데이터 집계 중입니다.",
                DateUtil.isMarketOpen()
        );
    }

    @Override
    public List<StockSearchResult> getNewListings() {
        LocalDate since = LocalDate.now().minusDays(30);
        return stockRepository.findNewListings(since).stream()
                .map(s -> new StockSearchResult(
                        s.getTicker(),
                        s.getName(),
                        s.getMarketType(),
                        s.getSector().getSectorName(),
                        s.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
