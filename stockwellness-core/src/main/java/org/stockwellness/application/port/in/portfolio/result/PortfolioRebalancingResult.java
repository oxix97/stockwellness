package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioRebalancingResult(
    BigDecimal totalValue,
    List<RebalancingItem> items
) {
    public record RebalancingItem(
        String symbol,
        BigDecimal currentWeight,
        BigDecimal targetWeight,
        BigDecimal diffWeight,
        BigDecimal currentQuantity,
        BigDecimal recommendedQuantity,
        BigDecimal currentPrice
    ) {}
}
