package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioInceptionPerformanceResult;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioInceptionPerformanceResponse(
    BigDecimal portfolioTotalReturn,
    BigDecimal benchmarkReturn,
    List<StockPerformanceResponse> stockPerformances
) {
    public static PortfolioInceptionPerformanceResponse from(PortfolioInceptionPerformanceResult result) {
        return new PortfolioInceptionPerformanceResponse(
            result.portfolioTotalReturn(),
            result.benchmarkReturn(),
            result.stockPerformances().stream()
                .map(p -> new StockPerformanceResponse(
                    p.symbol(),
                    p.name(),
                    p.individualReturn(),
                    p.contribution(),
                    p.relativePerformance()
                )).toList()
        );
    }

    public record StockPerformanceResponse(
        String symbol,
        String name,
        BigDecimal individualReturn,
        BigDecimal contribution,
        BigDecimal relativePerformance
    ) {}
}
