package org.stockwellness.application.port.in.portfolio.dto;

import java.math.BigDecimal;

import org.stockwellness.domain.portfolio.AssetType;

public record PortfolioItemRequest(
    String symbol,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    String currency,
    AssetType assetType,
    BigDecimal targetWeight
) {}
