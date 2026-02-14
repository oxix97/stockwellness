package org.stockwellness.adapter.in.web.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.in.stock.PopularSearchUseCase;
import org.stockwellness.application.port.in.stock.SearchHistoryUseCase;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockDiscoveryController {

    private final StockUseCase stockUseCase;
    private final SearchHistoryUseCase searchHistoryUseCase;
    private final PopularSearchUseCase popularSearchUseCase;

    /**
     * 통합 종목 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Slice<StockSearchResult>> searchStocks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketType marketType,
            @RequestParam(required = false) StockStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            // TODO: @AuthenticationPrincipal 활용하여 memberId 주입
    ) {
        // 임시 memberId (테스트용)
        Long memberId = 1L;
        
        SearchStockQuery query = new SearchStockQuery(keyword, marketType, status, page, size);
        Slice<StockSearchResult> result = stockUseCase.searchStocks(query);
        
        // 검색 성공 시 히스토리에 저장 (비동기 처리 권장되나 현재는 직접 호출)
        if (keyword != null && !keyword.isBlank()) {
            searchHistoryUseCase.saveSearchHistory(memberId, keyword);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 최근 검색어 조회
     */
    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getRecentSearches() {
        Long memberId = 1L; // 임시
        return ResponseEntity.ok(searchHistoryUseCase.getRecentSearches(memberId));
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @DeleteMapping("/search/history")
    public ResponseEntity<Void> removeSearchHistory(@RequestParam String keyword) {
        Long memberId = 1L; // 임시
        searchHistoryUseCase.removeSearchHistory(memberId, keyword);
        return ResponseEntity.noContent().build();
    }

    /**
     * 최근 검색어 전체 삭제
     */
    @DeleteMapping("/search/history/all")
    public ResponseEntity<Void> clearSearchHistory() {
        Long memberId = 1L; // 임시
        searchHistoryUseCase.clearSearchHistory(memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 인기 검색어 Top 10 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularSearches() {
        return ResponseEntity.ok(popularSearchUseCase.getPopularSearches());
    }

    /**
     * 신규 상장 종목 조회
     */
    @GetMapping("/new-listings")
    public ResponseEntity<List<StockSearchResult>> getNewListings() {
        return ResponseEntity.ok(stockUseCase.getNewListings());
    }
}
