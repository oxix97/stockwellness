package org.stockwellness.application.port.in.member;

import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.application.port.in.member.result.MemberResult;

public interface MemberUseCase {
    MemberResult getMember(Long memberId);

    void updateMember(Long memberId, UpdateMemberCommand command);

    void withdrawMember(Long memberId);
}
