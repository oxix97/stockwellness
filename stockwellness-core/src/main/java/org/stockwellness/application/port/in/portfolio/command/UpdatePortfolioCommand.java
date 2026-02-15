package org.stockwellness.application.port.in.portfolio.command;

import org.stockwellness.domain.portfolio.AssetType;

import java.util.List;

public record UpdatePortfolioCommand(
    Long memberId,
    Long portfolioId,
    String name,
    String description,
    List<PortfolioItemCommand> items
) {
    public record PortfolioItemCommand(
        String stockCode,
        int pieceCount,
        AssetType assetType
    ) {}
}
