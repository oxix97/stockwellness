package org.stockwellness.adapter.out.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.shared.Email;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private JwtProperties jwtProperties;
    private String secretKeyPlain = "test-secret-key-test-secret-key-test-secret-key";
    private String secretKeyBase64;

    @Mock
    private Member member;

    @BeforeEach
    void setUp() {
        secretKeyBase64 = Base64.getEncoder().encodeToString(secretKeyPlain.getBytes());
        jwtProperties = new JwtProperties(secretKeyBase64, 3600000L, 86400000L);
        jwtProvider = new JwtProvider(jwtProperties);
        jwtProvider.init();
    }

    @Test
    @DisplayName("Access Token 발급 시 Claims 정보가 올바르게 들어간다")
    void generateAccessToken_success() {
        // given
        Long memberId = 1L;
        String email = "test@example.com";
        MemberRole role = MemberRole.USER;
        LoginType loginType = LoginType.GOOGLE;

        given(member.getId()).willReturn(memberId);
        given(member.getEmail()).willReturn(new Email(email));
        given(member.getRole()).willReturn(role);
        given(member.getLoginType()).willReturn(loginType);

        // when
        String token = jwtProvider.generateAccessToken(member);

        // then
        assertThat(token).isNotNull();

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
        assertThat(claims.get("email")).isEqualTo(email);
        assertThat(claims.get("role")).isEqualTo(role.name());
        assertThat(claims.get("loginType")).isEqualTo(loginType.name());
    }

    @Test
    @DisplayName("Refresh Token 발급이 정상적으로 수행된다")
    void generateRefreshToken_success() {
        // given
        Long memberId = 1L;
        given(member.getId()).willReturn(memberId);

        // when
        String refreshToken = jwtProvider.generateRefreshToken(member);

        // then
        assertThat(refreshToken).isNotNull();

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64)))
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
    }

    @Test
    @DisplayName("토큰에서 Member ID를 정상적으로 추출한다")
    void extractMemberId_success() {
        // given
        Long memberId = 100L;
        given(member.getId()).willReturn(memberId);
        String token = jwtProvider.generateRefreshToken(member);

        // when
        Long extractedId = jwtProvider.extractMemberId(token);

        // then
        assertThat(extractedId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 true를 반환한다")
    void isTokenValid_success() {
        // given
        given(member.getId()).willReturn(1L);
        String token = jwtProvider.generateRefreshToken(member);

        // when
        boolean isValid = jwtProvider.isTokenValid(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("조작된 토큰 검증 시 false를 반환한다")
    void isTokenValid_fail_tampered() {
        // given
        given(member.getId()).willReturn(1L);
        String validToken = jwtProvider.generateRefreshToken(member);
        String tamperedToken = validToken + "d";

        // when
        boolean isValid = jwtProvider.isTokenValid(tamperedToken);

        // then
        assertThat(isValid).isFalse();
    }
}