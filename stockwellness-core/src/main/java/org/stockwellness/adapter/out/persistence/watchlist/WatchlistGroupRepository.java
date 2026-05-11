package org.stockwellness.adapter.out.persistence.watchlist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.watchlist.WatchlistGroup;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long>, WatchlistCustomRepository {
    List<WatchlistGroup> findAllByMemberIdAndDeletedAtIsNull(Long memberId);
    long countByMemberIdAndDeletedAtIsNull(Long memberId);
}
