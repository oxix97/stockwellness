package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record PortfolioRebalancingResponse(
    BigDecimal totalValue,
    List<RebalancingItemResponse> items
) {
    public record RebalancingItemResponse(
        String symbol,
        String name,
        BigDecimal currentWeight,
        BigDecimal targetWeight,
        BigDecimal diffWeight,
        BigDecimal currentQuantity,
        BigDecimal recommendedQuantity,
        BigDecimal currentPrice,
        BigDecimal expectedTradeAmount
    ) {}

    public static PortfolioRebalancingResponse from(PortfolioRebalancingResult result) {
        return new PortfolioRebalancingResponse(
            result.totalValue().setScale(0, RoundingMode.HALF_UP),
            result.items().stream()
                .map(i -> new RebalancingItemResponse(
                    i.symbol(),
                    i.name(),
                    i.currentWeight().setScale(4, RoundingMode.HALF_UP),
                    i.targetWeight().setScale(4, RoundingMode.HALF_UP),
                    i.diffWeight().setScale(4, RoundingMode.HALF_UP),
                    i.currentQuantity().setScale(4, RoundingMode.HALF_UP), // Quantities can be fractional
                    i.recommendedQuantity().setScale(4, RoundingMode.HALF_UP),
                    i.currentPrice().setScale(0, RoundingMode.HALF_UP),
                    i.recommendedQuantity().multiply(i.currentPrice()).setScale(0, RoundingMode.HALF_UP)
                ))
                .toList()
        );
    }
}
