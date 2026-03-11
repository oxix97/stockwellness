package org.stockwellness.adapter.in.web.portfolio.dto;

import java.util.List;

public record PortfolioAnalysisSummaryResponse(
    PortfolioValuationResponse valuation,
    PortfolioDiversificationResponse diversification,
    PortfolioRebalancingResponse rebalancing
) {}
