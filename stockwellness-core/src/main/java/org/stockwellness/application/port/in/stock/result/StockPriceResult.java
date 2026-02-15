package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉 데이터 전달용 불변 객체
 */
public record StockPriceResult(
        LocalDate baseDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal adjClosePrice,
        Long volume
) {
}
