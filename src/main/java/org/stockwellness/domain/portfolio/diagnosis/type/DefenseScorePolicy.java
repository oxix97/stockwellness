package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DefenseScorePolicy {
    MEGA_CAP(20_000_000_000_000L, 100),
    LARGE_CAP(5_000_000_000_000L, 80),
    MID_CAP(1_000_000_000_000L, 60),
    SMALL_CAP(0L, 40);

    private final long threshold;
    private final int score;

    public static int calculate(BigDecimal marketCap) {
        if (marketCap == null) return 50; // Default
        long value = marketCap.longValue();
        return Arrays.stream(values())
                .filter(policy -> value >= policy.threshold)
                .findFirst()
                .map(DefenseScorePolicy::getScore)
                .orElse(SMALL_CAP.score);
    }
}
