package org.stockwellness.domain.stock.insight;

public record MarketWeatherScore(
    int trendScore,
    int breadthScore,
    int momentumScore,
    int integratedScore
) {
}
