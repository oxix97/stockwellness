package org.stockwellness.adapter.in.event.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.member.SaveMemberPort;
import org.stockwellness.application.port.out.watchlist.WatchlistPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.event.MemberCreatedEvent;
import org.stockwellness.domain.watchlist.WatchlistGroup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberCreatedEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WatchlistPort watchlistPort;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Test
    @DisplayName("회원 가입 이벤트 발생 시 기본 관심 그룹이 생성된다")
    void handleMemberCreatedEvent() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        saveMemberPort.saveMember(member);

        // when
        eventPublisher.publishEvent(new MemberCreatedEvent(member));

        // then
        List<WatchlistGroup> groups = watchlistPort.findAllGroupsByMemberId(member.getId());
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getName()).isEqualTo("기본 그룹");
    }
}
