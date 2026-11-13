package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockTechnicalIndicator(
        long stockId,
        LocalDate tradeDate,
        String quoteScope,
        PriceBasis priceBasis,
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
    public StockTechnicalIndicator {
        if (stockId <= 0 || tradeDate == null || quoteScope == null || quoteScope.isBlank()
                || quoteScope.length() > 20 || priceBasis == null || inputManifest == null || inputManifest.isBlank()
                || algorithmVersion == null || algorithmVersion.isBlank() || algorithmVersion.length() > 80
                || calculatedAt == null) {
            throw new IllegalArgumentException("Stock indicator identity and input provenance are required");
        }
    }
}
