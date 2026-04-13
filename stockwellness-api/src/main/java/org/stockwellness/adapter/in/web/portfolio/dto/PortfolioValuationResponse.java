package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PortfolioValuationResponse(
    BigDecimal totalPurchaseAmount,
    BigDecimal currentTotalValue,
    BigDecimal totalProfitLoss,
    BigDecimal totalReturnRate,
    BigDecimal dailyProfitLoss,
    BigDecimal dailyReturnRate,
    BigDecimal cagr,
    BigDecimal volatility,
    BigDecimal alpha,
    BigDecimal mdd,
    BigDecimal sharpeRatio,
    BigDecimal beta,
    BigDecimal totalInstitutionalNetBuying,
    BigDecimal totalForeignNetBuying,
    BigDecimal totalPersonNetBuying
) {
    public static PortfolioValuationResponse from(PortfolioValuationResult result) {
        return new PortfolioValuationResponse(
            result.totalPurchaseAmount().setScale(0, RoundingMode.HALF_UP),
            result.currentTotalValue().setScale(0, RoundingMode.HALF_UP),
            result.totalProfitLoss().setScale(0, RoundingMode.HALF_UP),
            result.totalReturnRate().setScale(4, RoundingMode.HALF_UP),
            result.dailyProfitLoss().setScale(0, RoundingMode.HALF_UP),
            result.dailyReturnRate().setScale(4, RoundingMode.HALF_UP),
            result.cagr().setScale(4, RoundingMode.HALF_UP),
            result.volatility().setScale(4, RoundingMode.HALF_UP),
            result.alpha().setScale(4, RoundingMode.HALF_UP),
            result.mdd().setScale(4, RoundingMode.HALF_UP),
            result.sharpeRatio().setScale(4, RoundingMode.HALF_UP),
            result.beta().setScale(4, RoundingMode.HALF_UP),
            result.totalInstitutionalNetBuying().setScale(0, RoundingMode.HALF_UP),
            result.totalForeignNetBuying().setScale(0, RoundingMode.HALF_UP),
            result.totalPersonNetBuying().setScale(0, RoundingMode.HALF_UP)
        );
    }
}
