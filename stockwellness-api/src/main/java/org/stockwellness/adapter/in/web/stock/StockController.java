package org.stockwellness.adapter.in.web.stock;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.in.stock.StockPriceUseCase;
import org.stockwellness.application.port.in.stock.StockPriceUseCase.ChartQuery;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.*;
import org.stockwellness.domain.stock.price.ChartFrequency;
import org.stockwellness.domain.stock.price.ChartPeriod;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.TradeDirection;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.common.response.SliceResponse;
import org.stockwellness.global.security.MemberPrincipal;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@Validated
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
    public ApiResponse<SliceResponse<StockSearchResult>> searchStocks(
            @AuthenticationPrincipal MemberPrincipal member,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketType marketType,
            @RequestParam(required = false) StockStatus status,
            @RequestParam(required = false) String sectorCode,
            @RequestParam(required = false) String sectorName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SearchStockQuery query = new SearchStockQuery(keyword, marketType, status, sectorCode, sectorName, page, size);
        Slice<StockSearchResult> result = stockUseCase.searchStocks(query);

        // 검색 성공 시 히스토리에 저장 (인증된 사용자만)
        if (member != null && keyword != null && keyword.matches("^[가-힣a-zA-Z0-9]{2,}$")) {
            stockSearchUseCase.saveSearchHistory(member.id(), keyword);
        }

        return ApiResponse.success(SliceResponse.from(result));
    }

    /**
     * 최근 검색어 조회
     */
    @GetMapping("/search/history")
    public ApiResponse<List<String>> getRecentSearches(
            @AuthenticationPrincipal MemberPrincipal member
    ) {
        if (member == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(stockSearchUseCase.getRecentSearches(member.id()));
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @DeleteMapping("/search/history")
    public ApiResponse<Void> removeSearchHistory(
            @AuthenticationPrincipal MemberPrincipal member,
            @RequestParam String keyword
    ) {
        if (member != null) {
            stockSearchUseCase.removeSearchHistory(member.id(), keyword);
        }
        return ApiResponse.success();
    }

    /**
     * 최근 검색어 전체 삭제
     */
    @DeleteMapping("/search/history/all")
    public ApiResponse<Void> clearSearchHistory(
            @AuthenticationPrincipal MemberPrincipal member
    ) {
        if (member != null) {
            stockSearchUseCase.clearSearchHistory(member.id());
        }
        return ApiResponse.success();
    }

    /**
     * 인기 검색어 Top 10 조회
     */
    @GetMapping("/popular-search")
    public ApiResponse<List<String>> getPopularSearches() {
        return ApiResponse.success(stockSearchUseCase.getPopularSearches());
    }

    /**
     * 신규 상장 종목 조회
     */
    @GetMapping("/new-listings")
    public ApiResponse<List<StockSearchResult>> getNewListings() {
        return ApiResponse.success(stockUseCase.getNewListings());
    }

    /**
     * 종목 상세 정보 조회
     */
    @GetMapping("/{ticker}")
    public ApiResponse<StockDetailResult> getStockDetail(
            @PathVariable String ticker
    ) {
        var result = stockUseCase.getStockDetail(ticker);
        return ApiResponse.success(result);
    }

    /**
     * 종목 수급 랭킹 조회 (가장 최신 날짜 기준)
     */
    @GetMapping("/ranking/supply")
    public ApiResponse<StockSupplyRankingResponse> getTopStocksBySupply(
            @RequestParam(defaultValue = "BUY") TradeDirection direction,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit는 1 이상이어야 합니다.") int limit
    ) {
        return ApiResponse.success(stockPriceUseCase.getTopStocksBySupply(direction, limit));
    }

    // ==========================================
    // Group B: 시세 데이터 및 차트 (Price & Chart)
    // ==========================================

    /**
     * 8, 14, 25. 차트용 과거 가격 데이터 조회 (기간별)
     */
    @GetMapping("/{ticker}/prices/history")
    public ApiResponse<ChartDataResponse> getPriceHistory(
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
        return ApiResponse.success(stockPriceUseCase.loadChartData(query));
    }

    /**
     * 15. 기간별 수익률 및 벤치마크 대비 수익률 조회
     */
    @GetMapping("/{ticker}/returns")
    public ApiResponse<ReturnRateResponse> getReturns(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1Y") String period
    ) {
        return ApiResponse.success(stockPriceUseCase.calculateReturn(
                ticker,
                ChartPeriod.fromLabel(period)
        ));
    }
}
