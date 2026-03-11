package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.AssetType;
import java.math.BigDecimal;

public record PortfolioItemRequest(
    String symbol,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    String currency,
    AssetType assetType,
    BigDecimal targetWeight
) {}
