package org.stockwellness.application.port.in.portfolio.command;

import java.math.BigDecimal;
import java.util.List;

import org.stockwellness.domain.portfolio.AssetType;

public record CreatePortfolioCommand(
    Long memberId,
    String name,
    String description,
    List<PortfolioItemCommand> items
) {
    public record PortfolioItemCommand(
        String symbol,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        String currency,
        AssetType assetType,
        BigDecimal targetWeight
    ) {}
}
