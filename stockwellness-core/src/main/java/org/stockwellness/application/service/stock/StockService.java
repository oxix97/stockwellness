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
import org.stockwellness.domain.stock.event.StockSearchEvent;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StockService implements StockUseCase {

    private final StockRepository stockRepository;
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
                s.getSectorLargeCode(),
                s.getStatus()
        ));
    }

    @Override
    public StockDetailResult getStockDetail(String ticker) {
        // 기존 상세 조회 로직 (현재 트랙 범위 아님, 필요 시 구현)
        return null;
    }

    @Override
    public List<StockSearchResult> getNewListings() {
        return stockRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(s -> new StockSearchResult(
                        s.getTicker(),
                        s.getName(),
                        s.getMarketType(),
                        s.getSectorLargeCode(),
                        s.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
