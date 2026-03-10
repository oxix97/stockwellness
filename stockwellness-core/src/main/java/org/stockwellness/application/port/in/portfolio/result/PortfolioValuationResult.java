package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;

public record PortfolioValuationResult(
    BigDecimal totalPurchaseAmount,
    BigDecimal currentTotalValue,
    BigDecimal totalProfitLoss,
    BigDecimal totalReturnRate,
    BigDecimal dailyProfitLoss,
    BigDecimal dailyReturnRate
) {}
