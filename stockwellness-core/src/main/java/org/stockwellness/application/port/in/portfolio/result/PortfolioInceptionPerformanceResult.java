package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioInceptionPerformanceResult(
    BigDecimal portfolioTotalReturn,
    BigDecimal benchmarkReturn,
    List<StockInceptionPerformance> stockPerformances
) {
    public record StockInceptionPerformance(
        String symbol,
        String name,
        BigDecimal individualReturn,
        BigDecimal contribution,
        BigDecimal relativePerformance
    ) {}
}
