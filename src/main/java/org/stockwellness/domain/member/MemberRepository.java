package org.stockwellness.domain.member;


import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.shared.Email;

import java.util.Optional;

/**
 * 회원 정보를 저장하거나 조회한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(Email email);

    Optional<Member> findByEmailAndLoginType(Email email, LoginType loginType);
}
