package org.stockwellness.adapter.out.persistence.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.watchlist.WatchlistPort;
import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WatchlistAdapter implements WatchlistPort {

    private final WatchlistGroupRepository watchlistGroupRepository;
    private final WatchlistItemRepository watchlistItemRepository;

    @Override
    public List<WatchlistGroup> findAllGroupsByMemberId(Long memberId) {
        return watchlistGroupRepository.findAllByMemberIdAndDeletedAtIsNull(memberId);
    }

    @Override
    public List<WatchlistGroupWithCount> findGroupsWithItemCount(Long memberId) {
        return watchlistGroupRepository.findGroupsWithItemCount(memberId);
    }

    @Override
    public List<WatchlistItem> findItemsWithStock(WatchlistGroup group) {
        return watchlistGroupRepository.findItemsWithStock(group);
    }

    @Override
    public long countGroupsByMemberId(Long memberId) {
        return watchlistGroupRepository.countByMemberIdAndDeletedAtIsNull(memberId);
    }

    @Override
    public long countItemsByGroup(WatchlistGroup group) {
        return watchlistItemRepository.countByGroupAndDeletedAtIsNull(group);
    }

    @Override
    public boolean existsItemByGroupAndStock(WatchlistGroup group, Stock stock) {
        return watchlistItemRepository.existsByGroupAndStockAndDeletedAtIsNull(group, stock);
    }

    @Override
    public Optional<WatchlistGroup> findGroupById(Long id) {
        return watchlistGroupRepository.findById(id);
    }

    @Override
    public WatchlistGroup saveGroup(WatchlistGroup group) {
        return watchlistGroupRepository.save(group);
    }

    @Override
    public Optional<WatchlistItem> findItemByGroupAndStock(WatchlistGroup group, String isinCode) {
        return watchlistItemRepository.findByGroupAndStockIsinCodeAndDeletedAtIsNull(group, isinCode);
    }
}