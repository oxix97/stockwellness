package org.stockwellness.adapter.out.persistence.watchlist;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.util.List;

import static org.stockwellness.domain.stock.QStock.stock;
import static org.stockwellness.domain.watchlist.QWatchlistGroup.watchlistGroup;
import static org.stockwellness.domain.watchlist.QWatchlistItem.watchlistItem;

@RequiredArgsConstructor
public class WatchlistCustomRepositoryImpl implements WatchlistCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<WatchlistGroupWithCount> findGroupsWithItemCount(Long memberId) {
        return queryFactory
                .select(Projections.constructor(WatchlistGroupWithCount.class,
                        watchlistGroup,
                        watchlistItem.count()
                ))
                .from(watchlistGroup)
                .leftJoin(watchlistItem).on(watchlistItem.group.eq(watchlistGroup).and(watchlistItem.deletedAt.isNull()))
                .where(
                        watchlistGroup.member.id.eq(memberId),
                        watchlistGroup.deletedAt.isNull()
                )
                .groupBy(watchlistGroup.id)
                .fetch();
    }

    @Override
    public List<WatchlistItem> findItemsWithStock(WatchlistGroup group) {
        return queryFactory
                .selectFrom(watchlistItem)
                .join(watchlistItem.stock, stock).fetchJoin()
                .where(
                        watchlistItem.group.eq(group),
                        watchlistItem.deletedAt.isNull()
                )
                .fetch();
    }
}
