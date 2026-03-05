package org.stockwellness.domain.stock.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주식 시세 업데이트 시 발행되는 이벤트
 */
public record StockPriceUpdatedEvent(
        List<String> symbols,
        LocalDateTime updatedAt
) {
    public static StockPriceUpdatedEvent of(List<String> symbols) {
        return new StockPriceUpdatedEvent(symbols, LocalDateTime.now());
    }
}
