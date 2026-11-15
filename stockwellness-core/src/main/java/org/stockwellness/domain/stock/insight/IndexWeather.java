package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IndexWeather(
        long seriesId,
        LocalDate tradeDate,
        BigDecimal score,
        String status,
        String scoreVersion,
        String inputManifest,
        Instant calculatedAt
) {
    public IndexWeather {
        if (seriesId <= 0 || tradeDate == null || score == null
                || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0
                || !"FINAL".equals(status) || scoreVersion == null || scoreVersion.isBlank()
                || scoreVersion.length() > 80 || inputManifest == null || inputManifest.isBlank()
                || calculatedAt == null) {
            throw new IllegalArgumentException("Index weather requires a complete final input result");
        }
    }
}
