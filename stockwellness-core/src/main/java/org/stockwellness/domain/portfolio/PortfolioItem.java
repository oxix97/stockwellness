package org.stockwellness.domain.portfolio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.stockwellness.domain.portfolio.exception.InvalidPortfolioException;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;

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
    private String symbol;

    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private String currency;

    public static PortfolioItem createStock(String symbol, BigDecimal quantity, BigDecimal purchasePrice, String currency) {
        validateQuantity(quantity);
        validatePrice(purchasePrice);
        PortfolioItem item = new PortfolioItem();
        item.symbol = symbol;
        item.assetType = AssetType.STOCK;
        item.quantity = quantity;
        item.purchasePrice = purchasePrice;
        item.currency = currency;
        return item;
    }

    public static PortfolioItem createCash(BigDecimal amount, String currency) {
        validateQuantity(amount);
        PortfolioItem item = new PortfolioItem();
        item.symbol = "CASH";
        item.assetType = AssetType.CASH;
        item.quantity = amount;
        item.purchasePrice = BigDecimal.ONE;
        item.currency = currency;
        return item;
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPortfolioException();
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPortfolioException();
        }
    }

    protected void assignPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public BigDecimal calculatePurchaseAmount() {
        return quantity.multiply(purchasePrice);
    }

    // Deprecated methods for backward compatibility during refactoring
    @Deprecated
    public String getIsinCode() {
        return symbol;
    }

    @Deprecated
    public Integer getPieceCount() {
        return quantity.intValue();
    }
}
