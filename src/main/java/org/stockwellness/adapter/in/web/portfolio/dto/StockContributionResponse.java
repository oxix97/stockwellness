package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.StockContributionResult;

public record StockContributionResponse(
        String name,
        String mainContribution,
        int score,
        String reason
) {
    public static StockContributionResponse from(StockContributionResult contribution) {
        return new StockContributionResponse(
                contribution.name(),
                contribution.mainContribution(),
                contribution.score(),
                contribution.reason()
        );
    }
}