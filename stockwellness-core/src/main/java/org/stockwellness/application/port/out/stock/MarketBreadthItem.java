package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시장 등락 분포(Market Breadth) 계산을 위한 최소 데이터 객체
 */
public record MarketBreadthItem(
        LocalDate baseDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal previousClosePrice
) {
}
