package org.stockwellness.domain.portfolio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.stockwellness.domain.portfolio.exception.PortfolioDomainException;
import org.stockwellness.domain.shared.AbstractEntity;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
@ToString
@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
public class PortfolioItem extends AbstractEntity {

    @ToString.Exclude
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(nullable = false)
    private String isinCode; // Cash인 경우 Cash

    @Enumerated(EnumType.STRING)
    private AssetType assetType; // Enum: STOCK, CASH

    @Column(nullable = false)
    private Integer pieceCount;

    public static PortfolioItem createStock(String stockCode, int pieceCount) {
        validatePieceCount(pieceCount);
        PortfolioItem item = new PortfolioItem();
        item.isinCode = stockCode;
        item.assetType = AssetType.STOCK;
        item.pieceCount = pieceCount;
        return item;
    }

    public static PortfolioItem createCash(int pieceCount) {
        validatePieceCount(pieceCount);
        PortfolioItem item = new PortfolioItem();
        item.isinCode = "KRW";
        item.assetType = AssetType.CASH;
        item.pieceCount = pieceCount;
        return item;
    }

    // 최소 1조각 검증
    private static void validatePieceCount(int pieceCount) {
        if (pieceCount < 1) {
            throw new PortfolioDomainException("각 종목은 최소 1조각 이상이어야 합니다.");
        }
    }

    protected void assignPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
}