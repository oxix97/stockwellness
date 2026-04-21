package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioInceptionChartResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PortfolioInceptionChartResponse(
        LocalDate portfolioInceptionDate,
        long daysElapsed,
        List<DailyResult> dailyResults,
        List<IndexComparisonResponse> comparisons
) {
    public record DailyResult(
            LocalDate date,
            BigDecimal portfolioReturnRate,
            Map<String, BigDecimal> benchmarkReturnRates
    ) {
    }

    public record IndexComparisonResponse(
            String indexName,
            String ticker,
            BigDecimal totalReturn
    ) {
    }

    public static PortfolioInceptionChartResponse from(PortfolioInceptionChartResult result) {
        return new PortfolioInceptionChartResponse(
                result.portfolioInceptionDate(),
                result.daysElapsed(),
                result.dailyResults().stream()
                        .map(daily -> new DailyResult(
                                daily.date(),
                                daily.portfolioReturnRate().setScale(4, RoundingMode.HALF_UP),
                                daily.benchmarkReturnRates().entrySet().stream()
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().setScale(4, RoundingMode.HALF_UP)))
                        ))
                        .toList(),
                result.comparisons().stream()
                        .map(comparison -> new IndexComparisonResponse(
                                comparison.indexName(),
                                comparison.ticker(),
                                comparison.totalReturn().setScale(4, RoundingMode.HALF_UP)
                        ))
                        .toList()
        );
    }
}
