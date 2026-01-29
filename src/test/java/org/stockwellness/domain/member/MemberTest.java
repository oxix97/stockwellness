package org.stockwellness.domain.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.stockwellness.domain.member.exception.MemberDomainException;
import org.stockwellness.domain.shared.Email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    @DisplayName("회원 등록 시 기본값들이 올바르게 설정되어야 한다.")
    void register_success() {
        // given
        String email = "test@example.com";
        String nickname = "tester";
        LoginType loginType = LoginType.GOOGLE;

        // when
        Member member = Member.register(email, nickname, loginType);

        // then
        assertThat(member.getEmail().getAddress()).isEqualTo(email);
        assertThat(member.getNickname()).isEqualTo(nickname);
        assertThat(member.getLoginType()).isEqualTo(loginType);

        // 기본값 검증
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("닉네임이 null이면 예외가 발생한다.")
    void register_fail_nickname_null() {
        assertThatThrownBy(() -> Member.register("test@a.com", null, LoginType.GOOGLE))
                .isInstanceOf(MemberDomainException.class)
                .hasMessage("닉네임은 null일 수 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("닉네임이 공백이면 예외가 발생한다.")
    void register_fail_nickname_blank(String invalidNickname) {
        assertThatThrownBy(() -> Member.register("test@a.com", invalidNickname, LoginType.GOOGLE))
                .isInstanceOf(MemberDomainException.class);
    }

    @Test
    @DisplayName("회원을 활성화하면 상태가 ACTIVE로 변경되어야 한다.")
    void activate_test() {
        // given
        Member member = Member.register("test@a.com", "tester", LoginType.GOOGLE);

        // when
        member.activate();

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.isActive()).isTrue();
    }

    @Nested
    class EmailTest {
        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "user.name@company.co.kr",
                "user_name@example.com",
                "user+tag@example.com",
                "123user@example.com"
        })
        @DisplayName("유효한 이메일 형식이면 객체가 정상적으로 생성된다.")
        void create_success(String validAddress) {
            // when
            Email email = new Email(validAddress);
            // then
            assertThat(email.getAddress()).isEqualTo(validAddress);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "plainaddress",          // @ 없음
                "@example.com",          // 사용자명 없음
                "user@",                 // 도메인 없음
                "user@example",          // TLD(.com 등) 없음
                "user@.com.my",          // 도메인 시작이 .
                "user name@example.com"  // 공백 포함
        })
        @DisplayName("이메일 형식이 올바르지 않으면 IllegalArgumentException이 발생한다.")
        void create_fail_invalid_format(String invalidAddress) {
            assertThatThrownBy(() -> new Email(invalidAddress))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 형식이 바르지 않습니다");
        }

        @Test
        @DisplayName("이메일 주소가 null이면 NullPointerException이 발생한다.")
        void create_fail_null() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
