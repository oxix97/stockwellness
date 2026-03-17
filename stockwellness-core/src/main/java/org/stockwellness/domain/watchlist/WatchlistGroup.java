package org.stockwellness.domain.watchlist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString(exclude = {"member", "items"})
@NoArgsConstructor(access = PROTECTED)
@Entity
public class WatchlistGroup extends AbstractEntity {

    public static final int MAX_GROUP_COUNT = 10;
    public static final int MAX_ITEM_COUNT = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<WatchlistItem> items = new ArrayList<>();

    private WatchlistGroup(Member member, String name) {
        validation(name);
        this.member = member;
        this.name = name;
    }

    public static WatchlistGroup create(Member member, String name) {
        return new WatchlistGroup(member, name);
    }

    public static WatchlistGroup createDefault(Member member) {
        return new WatchlistGroup(member, "기본 그룹");
    }

    public void rename(String newName) {
        validation(newName);
        this.name = newName;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.items.forEach(WatchlistItem::delete);
        this.items.clear();
    }

    public void addItem(Stock stock) {
        addItem(stock, null);
    }

    public void addItem(Stock stock, String note) {
        validateItemLimit();
        validateDuplicateItem(stock);

        WatchlistItem item = WatchlistItem.create(this, stock, note);
        this.items.add(item);
    }

    public void updateItemNote(String ticker, String newNote) {
        WatchlistItem item = findItemByTicker(ticker);
        item.updateNote(newNote);
    }

    public void removeItem(String ticker) {
        WatchlistItem itemToRemove = findItemByTicker(ticker);
        itemToRemove.delete();
        this.items.remove(itemToRemove);
    }

    private WatchlistItem findItemByTicker(String ticker) {
        return this.items.stream()
                .filter(item -> item.getTicker().equals(ticker) && item.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateItemLimit() {
        long activeCount = this.items.stream()
                .filter(item -> item.getDeletedAt() == null)
                .count();
        if (activeCount >= MAX_ITEM_COUNT) {
            throw new GlobalException(ErrorCode.WATCHLIST_ITEM_LIMIT_EXCEEDED);
        }
    }

    private void validateDuplicateItem(Stock stock) {
        boolean isDuplicate = this.items.stream()
                .anyMatch(item -> item.getTicker().equals(stock.getTicker()) && item.getDeletedAt() == null);
        if (isDuplicate) {
            throw new GlobalException(ErrorCode.DUPLICATE_WATCHLIST_ITEM);
        }
    }

    private void validation(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new GlobalException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public static void validateGroupCount(long currentCount) {
        if (currentCount >= MAX_GROUP_COUNT) {
            throw new GlobalException(ErrorCode.WATCHLIST_GROUP_LIMIT_EXCEEDED);
        }
    }
}
