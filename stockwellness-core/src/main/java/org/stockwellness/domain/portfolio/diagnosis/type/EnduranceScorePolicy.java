package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum EnduranceScorePolicy {
    OPTIMAL(100, 110, 100),
    OVERHEATED(110, Integer.MAX_VALUE, 70),
    UNDERVALUED(Integer.MIN_VALUE, 100, 40);

    private final int minPercent;
    private final int maxPercent;
    private final int score;

    public static int calculate(BigDecimal price, BigDecimal ma120) {
        if (price == null || ma120 == null || ma120.compareTo(BigDecimal.ZERO) == 0) return 50;

        BigDecimal disparity = price.multiply(new BigDecimal("100")).divide(ma120, 2, RoundingMode.HALF_UP);
        int value = disparity.intValue();

        if (value >= OPTIMAL.minPercent && value <= OPTIMAL.maxPercent) return OPTIMAL.score;
        if (value > OVERHEATED.minPercent) return OVERHEATED.score;
        return UNDERVALUED.score;
    }
}
