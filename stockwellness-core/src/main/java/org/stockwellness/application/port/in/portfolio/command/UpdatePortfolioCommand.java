package org.stockwellness.application.port.in.portfolio.command;

import org.stockwellness.domain.portfolio.AssetType;
import java.math.BigDecimal;
import java.util.List;

public record UpdatePortfolioCommand(
    Long memberId,
    Long portfolioId,
    String name,
    String description,
    List<PortfolioItemCommand> items
) {
    public record PortfolioItemCommand(
        String symbol,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        String currency,
        AssetType assetType
    ) {}
}
