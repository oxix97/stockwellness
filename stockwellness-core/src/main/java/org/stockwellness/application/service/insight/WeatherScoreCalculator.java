package org.stockwellness.application.service.insight;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class WeatherScoreCalculator {

    public int calculate(BigDecimal trendScore, BigDecimal momentumScore, BigDecimal breadthScore, BigDecimal volatilityScore) {
        BigDecimal total = trendScore.multiply(new BigDecimal("0.4"))
                .add(momentumScore.multiply(new BigDecimal("0.2")))
                .add(breadthScore.multiply(new BigDecimal("0.3")))
                .add(volatilityScore.multiply(new BigDecimal("0.1")));
        
        return total.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    // Helper to convert raw metrics to 0-100 scores
    public BigDecimal normalizeTrend(BigDecimal price, BigDecimal ma60) {
        if (ma60 == null || ma60.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("50");
        BigDecimal ratio = price.divide(ma60, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(new BigDecimal("100"));
        // Example: +10% above ma60 -> 100, -10% below -> 0
        BigDecimal score = ratio.add(new BigDecimal("5")).multiply(new BigDecimal("5"));
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }
}
