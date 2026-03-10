package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;

import java.math.BigDecimal;

public record PortfolioValuationResponse(
    BigDecimal totalPurchaseAmount,
    BigDecimal currentTotalValue,
    BigDecimal totalProfitLoss,
    BigDecimal totalReturnRate,
    BigDecimal dailyProfitLoss,
    BigDecimal dailyReturnRate
) {
    public static PortfolioValuationResponse from(PortfolioValuationResult result) {
        return new PortfolioValuationResponse(
            result.totalPurchaseAmount(),
            result.currentTotalValue(),
            result.totalProfitLoss(),
            result.totalReturnRate(),
            result.dailyProfitLoss(),
            result.dailyReturnRate()
        );
    }
}
