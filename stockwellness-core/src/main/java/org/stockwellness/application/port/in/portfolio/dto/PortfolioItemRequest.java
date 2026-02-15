package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.AssetType;

public record PortfolioItemRequest(String stockCode, int pieceCount, AssetType assetType) {}
