package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;

import java.util.List;
import java.util.Map;

public record DiagnosisResponse(
        int overallScore,
        List<ChartData> categories,
        List<StockContributionResponse> stockContributions,
        String summary,
        String insight,
        List<String> nextSteps
) {
    public record ChartData(String name, int value) {}

    public static DiagnosisResponse from(PortfolioHealthResult health) {
        return new DiagnosisResponse(
                health.overallScore(),
                health.categories().entrySet().stream()
                        .map(e -> new ChartData(e.getKey(), e.getValue()))
                        .toList(),
                health.stockContributions().stream()
                        .map(StockContributionResponse::from)
                        .toList(),
                health.summary(),
                health.insight(),
                health.nextSteps()
        );
    }
}