package org.stockwellness.adapter.in.web.stock.dto;


import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;

public record StockSearchRequest(
        String keyword,         // 검색어 (종목명 or 티커)
        MarketType marketType,  // 시장 구분 (KOSPI, KOSDAQ)
        StockStatus status,     // 상장 상태 (ACTIVE, DELISTED)

        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        Integer page,           // 페이지 번호 (1부터 시작)

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        Integer size            // 페이지 크기
) {
    // Compact Constructor: 기본값 설정 및 유효성 보정
    public StockSearchRequest {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20; // Default size 20
    }

    // 서비스 계층으로 넘길 때 Spring Data Pageable로 변환
    public Pageable toPageable() {
        // 사용자는 1페이지부터 시작하지만, Spring Data는 0부터 시작
        return PageRequest.of(page - 1, size, Sort.by("name").ascending());
    }

    // 도메인 쿼리 객체로 변환
    public SearchStockQuery toQuery() {
        return new SearchStockQuery(keyword, marketType, status, page, size);
    }
}