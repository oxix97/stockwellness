package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioRebalancingResponse(
    BigDecimal totalValue,
    List<RebalancingItemResponse> items
) {
    public record RebalancingItemResponse(
        String symbol,
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
            result.totalValue(),
            result.items().stream()
                .map(i -> new RebalancingItemResponse(
                    i.symbol(),
                    i.currentWeight(),
                    i.targetWeight(),
                    i.diffWeight(),
                    i.currentQuantity(),
                    i.recommendedQuantity(),
                    i.currentPrice(),
                    i.recommendedQuantity().multiply(i.currentPrice())
                ))
                .toList()
        );
    }
}
