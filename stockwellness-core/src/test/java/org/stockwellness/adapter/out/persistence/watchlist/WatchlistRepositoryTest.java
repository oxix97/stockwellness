package org.stockwellness.adapter.out.persistence.watchlist;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@DisplayName("WatchlistRepository 통합 테스트")
class WatchlistRepositoryTest {

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.register("watchlist@test.com", "watcher", LoginType.GOOGLE);
        memberRepository.save(member);
        memberRepository.flush();
    }

    @Test
    @DisplayName("회원의 활성 관심종목 그룹 목록을 조회한다")
    void findAllByMemberIdAndDeletedAtIsNull_Success() {
        // given
        WatchlistGroup g1 = WatchlistGroup.create(member, "그룹1");
        WatchlistGroup g2 = WatchlistGroup.create(member, "그룹2");
        WatchlistGroup g3 = WatchlistGroup.create(member, "삭제된그룹");
        g3.delete(); // Soft delete simulation if applicable, or just check the logic
        
        watchlistGroupRepository.saveAll(List.of(g1, g2, g3));
        watchlistGroupRepository.flush();

        // when
        List<WatchlistGroup> groups = watchlistGroupRepository.findAllByMemberIdAndDeletedAtIsNull(member.getId());

        // then
        assertThat(groups).hasSize(2);
        assertThat(groups).extracting(WatchlistGroup::getName).containsExactlyInAnyOrder("그룹1", "그룹2");
    }

    @Test
    @DisplayName("회원의 활성 관심종목 그룹 개수를 확인한다")
    void countByMemberIdAndDeletedAtIsNull_Success() {
        // given
        watchlistGroupRepository.save(WatchlistGroup.create(member, "그룹1"));
        watchlistGroupRepository.flush();

        // when
        long count = watchlistGroupRepository.countByMemberIdAndDeletedAtIsNull(member.getId());

        // then
        assertThat(count).isEqualTo(1);
    }
}
