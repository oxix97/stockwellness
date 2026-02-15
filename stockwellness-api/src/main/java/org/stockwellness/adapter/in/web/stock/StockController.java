package org.stockwellness.adapter.in.web.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.in.stock.StockPriceUseCase;
import org.stockwellness.application.port.in.stock.StockPriceUseCase.ChartQuery;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.domain.stock.ChartFrequency;
import org.stockwellness.domain.stock.ChartPeriod;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.global.security.MemberPrincipal;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;
    private final StockPriceUseCase stockPriceUseCase;
    private final StockSearchUseCase stockSearchUseCase;

    // ==========================================
    // Group A: 종목 탐색 및 기본 정보 (Discovery & Info)
    // ==========================================

    /**
     * 통합 종목 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Slice<StockSearchResult>> searchStocks(
            @AuthenticationPrincipal MemberPrincipal member,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketType marketType,
            @RequestParam(required = false) StockStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SearchStockQuery query = new SearchStockQuery(keyword, marketType, status, page, size);
        Slice<StockSearchResult> result = stockUseCase.searchStocks(query);

        // 검색 성공 시 히스토리에 저장 (인증된 사용자만)
        if (member != null && keyword != null && !keyword.isBlank()) {
            stockSearchUseCase.saveSearchHistory(member.id(), keyword);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 최근 검색어 조회
     */
    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getRecentSearches(
            @AuthenticationPrincipal MemberPrincipal member
    ) {
        if (member == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(stockSearchUseCase.getRecentSearches(member.id()));
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @DeleteMapping("/search/history")
    public ResponseEntity<Void> removeSearchHistory(
            @AuthenticationPrincipal MemberPrincipal member,
            @RequestParam String keyword
    ) {
        if (member != null) {
            stockSearchUseCase.removeSearchHistory(member.id(), keyword);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 최근 검색어 전체 삭제
     */
    @DeleteMapping("/search/history/all")
    public ResponseEntity<Void> clearSearchHistory(
            @AuthenticationPrincipal MemberPrincipal member
    ) {
        if (member != null) {
            stockSearchUseCase.clearSearchHistory(member.id());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 인기 검색어 Top 10 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularSearches() {
        return ResponseEntity.ok(stockSearchUseCase.getPopularSearches());
    }

    /**
     * 신규 상장 종목 조회
     */
    @GetMapping("/new-listings")
    public ResponseEntity<List<StockSearchResult>> getNewListings() {
        return ResponseEntity.ok(stockUseCase.getNewListings());
    }

    /**
     * 종목 상세 정보 조회
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<StockDetailResult> getStockDetail(
            @PathVariable String ticker
    ) {
        var result = stockUseCase.getStockDetail(ticker);
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // Group B: 시세 데이터 및 차트 (Price & Chart)
    // ==========================================

    /**
     * 8, 14, 25. 차트용 과거 가격 데이터 조회 (기간별)
     */
    @GetMapping("/{ticker}/prices/history")
    public ResponseEntity<ChartDataResponse> getPriceHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1Y") String period,
            @RequestParam(defaultValue = "DAILY") String frequency,
            @RequestParam(defaultValue = "false") boolean includeBenchmark
    ) {
        ChartQuery query = new ChartQuery(
                ticker,
                ChartPeriod.fromLabel(period),
                ChartFrequency.fromString(frequency),
                includeBenchmark
        );
        return ResponseEntity.ok(stockPriceUseCase.loadChartData(query));
    }

    /**
     * 15. 기간별 수익률 및 벤치마크 대비 수익률 조회
     */
    @GetMapping("/{ticker}/returns")
    public ResponseEntity<ReturnRateResponse> getReturns(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1Y") String period
    ) {
        return ResponseEntity.ok(stockPriceUseCase.calculateReturn(
                ticker,
                ChartPeriod.fromLabel(period)
        ));
    }
}
