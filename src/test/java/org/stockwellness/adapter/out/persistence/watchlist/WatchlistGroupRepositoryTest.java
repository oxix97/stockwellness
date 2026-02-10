package org.stockwellness.adapter.out.persistence.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.watchlist.WatchlistGroup;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, TestJpaConfig.class})
class WatchlistGroupRepositoryTest {

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("멤버 ID로 그룹 목록을 조회한다 (삭제되지 않은 그룹만)")
    void findAllByMemberIdAndDeletedAtIsNull() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        memberRepository.save(member);

        WatchlistGroup group1 = WatchlistGroup.create(member, "그룹 1");
        WatchlistGroup group2 = WatchlistGroup.create(member, "그룹 2");
        WatchlistGroup deletedGroup = WatchlistGroup.create(member, "삭제된 그룹");
        deletedGroup.delete();

        watchlistGroupRepository.saveAll(List.of(group1, group2, deletedGroup));

        // when
        List<WatchlistGroup> groups = watchlistGroupRepository.findAllByMemberIdAndDeletedAtIsNull(member.getId());

        // then
        assertThat(groups).hasSize(2)
                .extracting("name")
                .containsExactlyInAnyOrder("그룹 1", "그룹 2");
    }

    @Test
    @DisplayName("그룹 저장 및 조회")
    void saveAndFind() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        memberRepository.save(member);
        
        WatchlistGroup group = WatchlistGroup.create(member, "테스트 그룹");
        watchlistGroupRepository.save(group);

        // when
        Optional<WatchlistGroup> foundGroup = watchlistGroupRepository.findById(group.getId());

        // then
        assertThat(foundGroup).isPresent();
        assertThat(foundGroup.get().getName()).isEqualTo("테스트 그룹");
        assertThat(foundGroup.get().getMember().getId()).isEqualTo(member.getId());
    }
}