package org.stockwellness.adapter.out.persistence.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findAllByGroupAndDeletedAtIsNull(WatchlistGroup group);
    long countByGroupAndDeletedAtIsNull(WatchlistGroup group);
    boolean existsByGroupAndStockAndDeletedAtIsNull(WatchlistGroup group, Stock stock);
    Optional<WatchlistItem> findByGroupAndStockTickerAndDeletedAtIsNull(WatchlistGroup group, String isinCode);
}