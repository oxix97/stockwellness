package org.stockwellness.adapter.in.web.portfolio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.stockwellness.application.port.in.portfolio.result.PortfolioInceptionPerformanceResult;

public record PortfolioInceptionPerformanceResponse(
    BigDecimal portfolioTotalReturn,
    BigDecimal benchmarkReturn,
    List<StockPerformanceResponse> stockPerformances
) {
    public static PortfolioInceptionPerformanceResponse from(PortfolioInceptionPerformanceResult result) {
        return new PortfolioInceptionPerformanceResponse(
            result.portfolioTotalReturn().setScale(4, RoundingMode.HALF_UP),
            result.benchmarkReturn().setScale(4, RoundingMode.HALF_UP),
            result.stockPerformances().stream()
                .map(p -> new StockPerformanceResponse(
                    p.symbol(),
                    p.name(),
                    p.individualReturn().setScale(4, RoundingMode.HALF_UP),
                    p.contribution().setScale(4, RoundingMode.HALF_UP),
                    p.relativePerformance().setScale(4, RoundingMode.HALF_UP)
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
