package org.stockwellness.adapter.out.persistence.member;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @EntityGraph(attributePaths = "identities")
    @Query("select distinct member from Member member join member.identities identity where identity.subject = :subject")
    List<Member> findByIdentitySubject(@Param("subject") String subject);

    @EntityGraph(attributePaths = "identities")
    @Query("select distinct member from Member member join member.identities identity "
            + "where identity.subject = :subject and identity.provider = :provider")
    Optional<Member> findByIdentitySubjectAndProvider(
            @Param("subject") String subject, @Param("provider") String provider);

    @EntityGraph(attributePaths = "identities")
    @Query("select member from Member member where member.id = :memberId")
    Optional<Member> findMemberWithIdentities(@Param("memberId") Long memberId);

    default Optional<Member> findByEmail(Email email) {
        return findByIdentitySubject(email.getAddress()).stream().findFirst();
    }

    default Optional<Member> findByEmailAndLoginType(Email email, LoginType loginType) {
        return findByIdentitySubjectAndProvider(email.getAddress(), loginType.name());
    }

    boolean existsByNickname(String nickname);
}
