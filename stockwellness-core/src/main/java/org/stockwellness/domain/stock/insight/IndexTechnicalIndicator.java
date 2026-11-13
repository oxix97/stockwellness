package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IndexTechnicalIndicator(
        long seriesId,
        LocalDate tradeDate,
        BigDecimal ma5,
        BigDecimal ma20,
        BigDecimal ma20Disparity,
        BigDecimal ma60,
        BigDecimal ma120,
        BigDecimal rsi14,
        BigDecimal macd,
        BigDecimal macdSignal,
        BigDecimal adx,
        BigDecimal plusDi,
        BigDecimal minusDi,
        String inputManifest,
        String algorithmVersion,
        Instant calculatedAt
) {
    public IndexTechnicalIndicator {
        if (seriesId <= 0 || tradeDate == null || inputManifest == null || inputManifest.isBlank()
                || algorithmVersion == null || algorithmVersion.isBlank() || algorithmVersion.length() > 80
                || calculatedAt == null) {
            throw new IllegalArgumentException("Index indicator identity and input provenance are required");
        }
    }
}
