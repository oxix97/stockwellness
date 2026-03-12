package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.PortfolioItem;
import java.math.BigDecimal;

public record PortfolioItemResponse(
    String symbol,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    String currency,
    AssetType assetType,
    BigDecimal purchaseAmount,
    BigDecimal targetWeight
) {
    public static PortfolioItemResponse from(PortfolioItem entity) {
        return new PortfolioItemResponse(
            entity.getSymbol(),
            entity.getQuantity(),
            entity.getPurchasePrice(),
            entity.getCurrency(),
            entity.getAssetType(),
            entity.calculatePurchaseAmount(),
            entity.getTargetWeight()
        );
    }
}
