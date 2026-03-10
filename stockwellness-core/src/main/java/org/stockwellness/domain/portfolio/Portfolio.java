package org.stockwellness.domain.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@ToString
@NoArgsConstructor(access = PROTECTED)
public class Portfolio extends AbstractEntity {

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String name;

    private String description;

    @ToString.Exclude
    @OneToMany(fetch = LAZY, mappedBy = "portfolio", cascade = ALL, orphanRemoval = true)
    private List<PortfolioItem> items = new ArrayList<>();

    public static Portfolio create(Long memberId, String name, String description) {
        Portfolio portfolio = new Portfolio();
        portfolio.memberId = memberId;
        portfolio.name = name;
        portfolio.description = description;
        return portfolio;
    }

    public void updateBasicInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void updateItems(List<PortfolioItem> newItems) {
        // Validation logic can be added here if needed (e.g., max items)
        this.items.clear();
        this.items.addAll(newItems);
        newItems.forEach(item -> item.assignPortfolio(this));
    }

    public BigDecimal calculateTotalPurchaseAmount() {
        return items.stream()
                .map(PortfolioItem::calculatePurchaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Deprecated
    public int getTotalPieces() {
        return items.stream().mapToInt(PortfolioItem::getPieceCount).sum();
    }
}
