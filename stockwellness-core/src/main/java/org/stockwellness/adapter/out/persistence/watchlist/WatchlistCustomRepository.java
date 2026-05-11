package org.stockwellness.adapter.out.persistence.watchlist;

import java.util.List;

import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

public interface WatchlistCustomRepository {
    List<WatchlistGroupWithCount> findGroupsWithItemCount(Long memberId);
    List<WatchlistItem> findItemsWithStock(WatchlistGroup group);
}
