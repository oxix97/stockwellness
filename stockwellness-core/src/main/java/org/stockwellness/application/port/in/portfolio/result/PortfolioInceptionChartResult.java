package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PortfolioInceptionChartResult(
        LocalDate portfolioInceptionDate,
        long daysElapsed,
        List<DailyResult> dailyResults,
        List<IndexComparison> comparisons
) {
    public record DailyResult(
            LocalDate date,
            BigDecimal portfolioReturnRate,
            Map<String, BigDecimal> benchmarkReturnRates
    ) {
    }

    public record IndexComparison(
            String indexName,
            String ticker,
            BigDecimal totalReturn
    ) {
    }
}
