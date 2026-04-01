package org.stockwellness.adapter.in.web.portfolio.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioAnalysisSummaryResponse(
    PortfolioValuationResponse valuation,
    PortfolioDiversificationResponse diversification,
    PortfolioRebalancingResponse rebalancing,
    Map<String, BigDecimal> itemContributions
) {}
