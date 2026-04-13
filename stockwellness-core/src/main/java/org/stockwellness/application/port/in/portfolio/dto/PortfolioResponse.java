package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record PortfolioResponse(
        Long id,
        String name,
        String description,
        BigDecimal totalPurchaseAmount,
        BigDecimal currentTotalValue,
        BigDecimal totalReturnRate,
        List<PortfolioItemResponse> items
) {
    public static PortfolioResponse from(Portfolio entity, Map<String, BigDecimal> latestPriceMap, Map<String, Stock> stockMap) {
        List<PortfolioItemResponse> itemResponses = entity.getItems().stream()
                .map(item -> {
                    BigDecimal latestPrice = latestPriceMap.getOrDefault(item.getSymbol(), BigDecimal.ZERO);
                    return PortfolioItemResponse.from(item, resolveDisplayName(item, stockMap), latestPrice);
                })
                .toList();

        BigDecimal currentTotalValue = itemResponses.stream()
                .map(PortfolioItemResponse::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchaseAmount = entity.calculateTotalPurchaseAmount();
        BigDecimal totalReturnRate = BigDecimal.ZERO;

        if (totalPurchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnRate = currentTotalValue.subtract(totalPurchaseAmount)
                    .divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                totalPurchaseAmount,
                currentTotalValue,
                totalReturnRate,
                itemResponses
        );
    }

    private static String resolveDisplayName(PortfolioItem item, Map<String, Stock> stockMap) {
        if (item.getAssetType().name().equals("CASH")) {
            return "현금";
        }

        Stock stock = stockMap.get(item.getSymbol());
        if (stock != null && stock.getName() != null && !stock.getName().isBlank()) {
            return stock.getName();
        }
        return item.getSymbol();
    }
}
