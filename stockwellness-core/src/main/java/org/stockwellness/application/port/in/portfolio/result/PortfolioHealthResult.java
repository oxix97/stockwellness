package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PortfolioHealthResult(
        int overallScore,
        Map<String, Integer> categories,
        List<StockContributionResult> stockContributions,
        BigDecimal mdd,
        BigDecimal relativeMdd,
        BigDecimal sharpeRatio,
        BigDecimal alpha,
        String summary,
        String insight,
        List<String> nextSteps
) {
}