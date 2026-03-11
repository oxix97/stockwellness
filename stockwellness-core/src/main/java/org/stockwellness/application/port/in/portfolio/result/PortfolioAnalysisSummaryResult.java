package org.stockwellness.application.port.in.portfolio.result;

public record PortfolioAnalysisSummaryResult(
    PortfolioValuationResult valuation,
    PortfolioDiversificationResult diversification,
    PortfolioRebalancingResult rebalancing
) {}
