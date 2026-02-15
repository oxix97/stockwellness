package org.stockwellness.application.port.out.member;

import org.stockwellness.domain.member.Member;

public interface SaveMemberPort {
    Member saveMember(Member member);
}
