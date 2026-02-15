package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.PortfolioItem;

public record PortfolioItemResponse(
    String stockCode,
    int pieceCount,
    AssetType assetType,
    int piece
) {
    public static PortfolioItemResponse from(PortfolioItem entity) {
        // 총 조각 수 대비 퍼센트 계산 로직은 필요시 추가 (여기서는 단순 매핑)
        return new PortfolioItemResponse(
            entity.getIsinCode(),
            entity.getPieceCount(),
            entity.getAssetType(),
            entity.getPieceCount()
        );
    }
}