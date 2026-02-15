package org.stockwellness.domain.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString(exclude = {"group", "stock"})
@NoArgsConstructor(access = PROTECTED)
@Entity
public class WatchlistItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_group_id", nullable = false)
    private WatchlistGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType;

    private LocalDateTime deletedAt;

    private WatchlistItem(WatchlistGroup group, Stock stock) {
        this.group = group;
        this.stock = stock;
        this.marketType = stock.getMarketType();
    }

    public static WatchlistItem create(WatchlistGroup group, Stock stock) {
        return new WatchlistItem(group, stock);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}