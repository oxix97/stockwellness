package org.stockwellness.adapter.out.persistence.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.shared.Email;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@DisplayName("MemberRepository 통합 테스트")
class MemberRepositoryTest {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 회원을 조회한다")
    void findByEmail_Success() {
        // given
        String emailStr = "test@stockwellness.com";
        Member member = Member.register(emailStr, "tester", LoginType.GOOGLE);
        memberRepository.save(member);
        memberRepository.flush();

        // when
        Optional<Member> found = memberRepository.findByEmail(new Email(emailStr));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail().getAddress()).isEqualTo(emailStr);
        assertThat(found.get().getNickname()).isEqualTo("tester");
        assertThat(found.get().getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이메일과 로그인 타입으로 회원을 조회한다")
    void findByEmailAndLoginType_Success() {
        // given
        String emailStr = "kakao@stockwellness.com";
        Member member = Member.register(emailStr, "kakao_user", LoginType.KAKAO);
        memberRepository.save(member);
        memberRepository.flush();

        // when
        Optional<Member> found = memberRepository.findByEmailAndLoginType(new Email(emailStr), LoginType.KAKAO);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getLoginType()).isEqualTo(LoginType.KAKAO);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional을 반환한다")
    void findByEmail_NotFound() {
        // when
        Optional<Member> found = memberRepository.findByEmail(new Email("nonexistent@test.com"));

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("닉네임 중복 여부를 확인한다")
    void existsByNickname_Success() {
        // given
        String nickname = "unique_nickname";
        Member member = Member.register("unique@test.com", nickname, LoginType.GOOGLE);
        memberRepository.save(member);
        memberRepository.flush();

        // when
        boolean exists = memberRepository.existsByNickname(nickname);
        boolean notExists = memberRepository.existsByNickname("other_nickname");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
