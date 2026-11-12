package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.event.StockSearchEvent;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.global.util.DateUtil;

import static org.stockwellness.global.error.ErrorCode.STOCK_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StockService implements StockUseCase {

    private final StockRepository stockRepository;
    private final StockPriceReadPort stockPriceReadPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Slice<StockSearchResult> searchStocks(SearchStockQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());
        
        // 검색 수행
        var result = stockRepository.searchByCondition(
                query.keyword(),
                query.marketType(),
                query.status(),
                query.sectorCode(),
                query.sectorName(),
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
                .orElseThrow(() -> new StockPriceException(STOCK_NOT_FOUND));

        String quoteScope = stock.getMarketType() == MarketType.KOSPI || stock.getMarketType() == MarketType.KOSDAQ
                ? "KRX" : null;
        List<PriceObservation> recentPrices = quoteScope == null ? List.of()
                : stockPriceReadPort.recent(Set.of(stock.getId()), quoteScope, LocalDate.now(), 2, PriceBasis.RAW)
                        .getOrDefault(stock.getId(), List.of());
        PriceObservation latestPrice = recentPrices.isEmpty() ? null : recentPrices.getFirst();

        if (latestPrice == null) {
            return new StockDetailResult(
                    stock.getStandardCode(),
                    stock.getTicker(),
                    stock.getName(),
                    stock.getSector().getSectorName(),
                    stock.getMarketType().name(),
                    stock.getListedShares(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "시세 데이터가 없어 분석할 수 없습니다.",
                    DateUtil.isMarketOpen()
            );
        }

        BigDecimal closePrice = latestPrice.closePrice();
        PriceObservation previousPrice = recentPrices.size() > 1 ? recentPrices.get(1) : null;
        BigDecimal priceChange = previousPrice == null ? null : closePrice.subtract(previousPrice.closePrice());
        BigDecimal fluctuationRate = previousPrice == null || previousPrice.closePrice().compareTo(BigDecimal.ZERO) == 0
                ? null : priceChange.divide(previousPrice.closePrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return new StockDetailResult(
                stock.getStandardCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getSector().getSectorName(),
                stock.getMarketType().name(),
                stock.getListedShares(),
                latestPrice.tradeDate(),
                closePrice,
                priceChange,
                fluctuationRate,
                latestPrice.openPrice(),
                latestPrice.highPrice(),
                latestPrice.lowPrice(),
                latestPrice.volume(),
                latestPrice.tradingAmount(),
                null,
                null,
                null,
                "기술적 지표 집계 중입니다.",
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
