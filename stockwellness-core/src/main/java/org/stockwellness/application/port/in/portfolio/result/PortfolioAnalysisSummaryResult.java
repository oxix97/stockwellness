package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioAnalysisSummaryResult(
    PortfolioValuationResult valuation,
    PortfolioDiversificationResult diversification,
    PortfolioRebalancingResult rebalancing,
    Map<String, BigDecimal> itemContributions
) {}
