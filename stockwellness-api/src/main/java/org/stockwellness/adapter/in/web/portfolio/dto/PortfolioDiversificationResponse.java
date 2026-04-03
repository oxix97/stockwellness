package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record PortfolioDiversificationResponse(
    BigDecimal totalValue,
    List<ChartData> assetRatios,
    List<ChartData> sectorRatios,
    List<ChartData> countryRatios
) {
    public record ChartData(String name, BigDecimal value) {}

    public static PortfolioDiversificationResponse from(PortfolioDiversificationResult result) {
        return new PortfolioDiversificationResponse(
            result.totalValue().setScale(0, RoundingMode.HALF_UP),
            mapToChartData(result.assetRatios()),
            mapToChartData(result.sectorRatios()),
            mapToChartData(result.countryRatios())
        );
    }

    private static List<ChartData> mapToChartData(Map<String, BigDecimal> ratios) {
        return ratios.entrySet().stream()
            .map(e -> new ChartData(e.getKey(), e.getValue().setScale(4, RoundingMode.HALF_UP)))
            .sorted((a, b) -> b.value().compareTo(a.value())) // Value Descending
            .toList();
    }
}
