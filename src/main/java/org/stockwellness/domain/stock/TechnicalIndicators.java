package org.stockwellness.domain.stock;

import java.math.BigDecimal;

public record TechnicalIndicators(
    BigDecimal ma5,
    BigDecimal ma20,
    BigDecimal rsi14,
    BigDecimal macd
) {
    // 빈 값 처리용 (데이터 부족 시 사용)
    public static TechnicalIndicators empty() {
        return new TechnicalIndicators(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}