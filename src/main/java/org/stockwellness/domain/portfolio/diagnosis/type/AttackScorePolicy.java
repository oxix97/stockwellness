package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AttackScorePolicy {
    STRONG_BUY(70.0, 90),
    BUY(50.0, 70),
    NEUTRAL(0.0, 40);

    private final double rsiThreshold;
    private final int score;

    public static final int MACD_BONUS = 10;

    public static int calculate(BigDecimal rsi, BigDecimal macd) {
        if (rsi == null || macd == null) return 50;
        
        double rsiValue = rsi.doubleValue();
        int rsiScore = Arrays.stream(values())
                .filter(policy -> rsiValue >= policy.rsiThreshold)
                .findFirst()
                .map(AttackScorePolicy::getScore)
                .orElse(NEUTRAL.score);

        int macdBonus = macd.compareTo(BigDecimal.ZERO) >= 0 ? MACD_BONUS : 0;
        
        return rsiScore + macdBonus;
    }
}
