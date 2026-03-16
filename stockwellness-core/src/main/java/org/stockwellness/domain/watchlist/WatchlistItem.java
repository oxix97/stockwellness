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
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

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

    @Column(nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType;

    @Column(length = 200)
    private String note;

    private LocalDateTime deletedAt;

    private WatchlistItem(WatchlistGroup group, Stock stock, String note) {
        validateNote(note);
        this.group = group;
        this.stock = stock;
        this.ticker = stock.getTicker();
        this.marketType = stock.getMarketType();
        this.note = note;
    }

    public static WatchlistItem create(WatchlistGroup group, Stock stock) {
        return new WatchlistItem(group, stock, null);
    }

    public static WatchlistItem create(WatchlistGroup group, Stock stock, String note) {
        return new WatchlistItem(group, stock, note);
    }

    public void updateNote(String newNote) {
        validateNote(newNote);
        this.note = newNote;
    }

    private void validateNote(String note) {
        if (note != null && note.length() > 200) {
            throw new GlobalException(ErrorCode.WATCHLIST_NOTE_LIMIT_EXCEEDED);
        }
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
