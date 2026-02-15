package org.stockwellness.application.port.in.portfolio.result;

public record StockContributionResult(
        String name,
        String mainContribution,
        int score,
        String reason
) {
}