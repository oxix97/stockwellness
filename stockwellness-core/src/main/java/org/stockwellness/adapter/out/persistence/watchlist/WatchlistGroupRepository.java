package org.stockwellness.adapter.out.persistence.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.watchlist.WatchlistGroup;

import java.util.List;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long>, WatchlistCustomRepository {
    List<WatchlistGroup> findAllByMemberIdAndDeletedAtIsNull(Long memberId);
    long countByMemberIdAndDeletedAtIsNull(Long memberId);
}
