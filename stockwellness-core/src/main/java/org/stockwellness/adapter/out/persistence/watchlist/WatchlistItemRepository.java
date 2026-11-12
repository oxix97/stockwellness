package org.stockwellness.adapter.out.persistence.watchlist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findAllByGroupAndDeletedAtIsNull(WatchlistGroup group);
    long countByGroupAndDeletedAtIsNull(WatchlistGroup group);
    boolean existsByGroupAndStockAndDeletedAtIsNull(WatchlistGroup group, Stock stock);
}
