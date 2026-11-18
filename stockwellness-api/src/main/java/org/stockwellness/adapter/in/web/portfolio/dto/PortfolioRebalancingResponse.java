package org.stockwellness.adapter.in.web.portfolio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;

public record PortfolioRebalancingResponse(
    @JsonInclude(JsonInclude.Include.ALWAYS)
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
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal recommendedQuantity,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal currentPrice,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal expectedTradeAmount
    ) {}

    public static PortfolioRebalancingResponse from(PortfolioRebalancingResult result) {
        return new PortfolioRebalancingResponse(
            scale(result.totalValue(), 0),
            result.items() == null ? List.of() : result.items().stream()
                .map(i -> {
                    BigDecimal recommendedQuantity = scale(i.recommendedQuantity(), 4);
                    BigDecimal currentPrice = scale(i.currentPrice(), 0);
                    BigDecimal expectedTradeAmount = (i.recommendedQuantity() != null && i.currentPrice() != null)
                        ? i.recommendedQuantity().multiply(i.currentPrice()).setScale(0, RoundingMode.HALF_UP)
                        : null;
                    return new RebalancingItemResponse(
                        i.symbol(),
                        i.name(),
                        scale(i.currentWeight(), 4),
                        scale(i.targetWeight(), 4),
                        scale(i.diffWeight(), 4),
                        scale(i.currentQuantity(), 4), // Quantities can be fractional
                        recommendedQuantity,
                        currentPrice,
                        expectedTradeAmount
                    );
                })
                .toList()
        );
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
