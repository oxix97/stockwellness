package org.stockwellness.domain.stock.event;

import java.time.LocalDateTime;

/**
 * 종목 검색 시 발행되는 이벤트
 */
public record StockSearchEvent(
        String keyword,
        Long memberId,
        LocalDateTime searchedAt
) {
    public static StockSearchEvent of(String keyword, Long memberId) {
        return new StockSearchEvent(keyword, memberId, LocalDateTime.now());
    }
}
