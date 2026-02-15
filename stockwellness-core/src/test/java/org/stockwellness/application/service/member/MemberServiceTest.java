package org.stockwellness.application.service.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.RiskLevel;
import org.stockwellness.fixture.MemberFixture;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Member 서비스 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private LoadMemberPort loadMemberPort;

    @Test
    @DisplayName("탈퇴한 회원의 정보를 수정하려고 하면 예외가 발생한다")
    void updateMember_fail_deactivated() {
        // given
        Long memberId = 1L;
        Member member = MemberFixture.createMember();
        member.deactivate();

        given(loadMemberPort.loadMember(memberId)).willReturn(Optional.of(member));

        UpdateMemberCommand command = new UpdateMemberCommand(memberId, "newNick", RiskLevel.HIGH);

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(memberId, command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }
}
