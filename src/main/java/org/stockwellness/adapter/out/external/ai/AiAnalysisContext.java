package org.stockwellness.adapter.out.external.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiAnalysisContext(
        String isinCode,
        LocalDate analysisDate,
        PriceSummary price,
        TechnicalSignal technical,
        PortfolioRisk risk
) {
    public record PriceSummary(
            BigDecimal closePrice,
            BigDecimal fluctuationRate
    ) {
    }

    public record TechnicalSignal(
            String trend,
            String rsiStatus,
            String macdSignal,
            double volatility
    ) {
    }

    public record PortfolioRisk(boolean isConcentrated, double cashWeight) {
    }
}