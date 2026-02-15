package org.stockwellness.application.port.out.member;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;

import java.util.Optional;

public interface LoadMemberPort {
    Optional<Member> loadMember(Long memberId);
    Optional<Member> loadMemberByEmail(Email email);
    Optional<Member> loadMemberByEmailAndLoginType(Email email, LoginType loginType);
    boolean existsByNickname(String nickname);
}
