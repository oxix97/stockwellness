package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;

public record MarketBreadthSnapshot(
        int total,
        int advancing,
        int declining,
        int unchanged,
        int highVolatility,
        BigDecimal advanceRatio,
        BigDecimal declineRatio,
        BigDecimal highVolatilityRatio
) {
}
