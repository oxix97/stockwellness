package org.stockwellness.domain.stock;

import java.math.BigDecimal;

public record TechnicalIndicators(
        BigDecimal ma5,
        BigDecimal ma20,
        BigDecimal ma60,
        BigDecimal ma120,
        BigDecimal rsi14,
        BigDecimal macd
) {
    // 빈 값 처리용 (데이터 부족 시 사용)
    public static TechnicalIndicators empty() {
        return new TechnicalIndicators(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static TechnicalIndicators of(
            BigDecimal ma5,
            BigDecimal ma20,
            BigDecimal ma60,
            BigDecimal ma120,
            BigDecimal rsi14,
            BigDecimal macd
    ) {

        return new TechnicalIndicators(ma5, ma20, ma60, ma120, rsi14, macd);
    }
}