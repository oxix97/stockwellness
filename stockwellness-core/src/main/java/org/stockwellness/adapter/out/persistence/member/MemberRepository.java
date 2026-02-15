package org.stockwellness.adapter.out.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(Email email);
    Optional<Member> findByEmailAndLoginType(Email email, LoginType loginType);
    boolean existsByNickname(String nickname);
}
