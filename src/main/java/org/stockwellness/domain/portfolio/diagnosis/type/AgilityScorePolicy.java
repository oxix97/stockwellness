package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AgilityScorePolicy {
    HIGH_VOLATILITY(5.0, 100),
    MID_VOLATILITY(2.0, 70),
    LOW_VOLATILITY(0.0, 40);

    private final double minFluctuation;
    private final int score;

    public static int calculate(double avgFluctuation) {
        if (avgFluctuation < 0) return 50; // Error case
        return Arrays.stream(values())
                .filter(policy -> avgFluctuation >= policy.minFluctuation)
                .findFirst()
                .map(AgilityScorePolicy::getScore)
                .orElse(LOW_VOLATILITY.score);
    }
}
