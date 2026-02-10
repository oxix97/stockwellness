package org.stockwellness.application.port.out.watchlist;

import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.util.List;
import java.util.Optional;

public interface WatchlistPort {
    List<WatchlistGroup> findAllGroupsByMemberId(Long memberId);
    List<WatchlistGroupWithCount> findGroupsWithItemCount(Long memberId);
    List<WatchlistItem> findItemsWithStock(WatchlistGroup group);

    long countGroupsByMemberId(Long memberId);

    long countItemsByGroup(WatchlistGroup group);

    boolean existsItemByGroupAndStock(WatchlistGroup group, Stock stock);

    Optional<WatchlistGroup> findGroupById(Long id);

    WatchlistGroup saveGroup(WatchlistGroup group);

    Optional<WatchlistItem> findItemByGroupAndStock(WatchlistGroup group, String isinCode);
}