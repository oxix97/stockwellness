package org.stockwellness.application.port.in.portfolio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.PortfolioItem;

public record PortfolioItemResponse(
    String symbol,
    String name,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    BigDecimal currentPrice,
    String currency,
    AssetType assetType,
    BigDecimal purchaseAmount,
    BigDecimal currentValue,
    BigDecimal returnRate,
    BigDecimal targetWeight
) {
    public static PortfolioItemResponse from(PortfolioItem entity, String name, BigDecimal currentPrice) {
        BigDecimal purchaseAmount = entity.calculatePurchaseAmount();
        BigDecimal currentValue = currentPrice.multiply(entity.getQuantity());
        BigDecimal returnRate = BigDecimal.ZERO;

        if (purchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            returnRate = currentValue.subtract(purchaseAmount)
                    .divide(purchaseAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioItemResponse(
            entity.getSymbol(),
            name,
            entity.getQuantity(),
            entity.getPurchasePrice(),
            currentPrice,
            entity.getCurrency(),
            entity.getAssetType(),
            purchaseAmount,
            currentValue,
            returnRate,
            entity.getTargetWeight()
        );
    }
}
