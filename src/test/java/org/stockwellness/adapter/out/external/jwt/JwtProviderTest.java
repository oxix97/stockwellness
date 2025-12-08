package org.stockwellness.adapter.out.external.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {
    @Mock
    Member member;

    String secretKey;
    JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 SecretKey 생성 (HS512 알고리즘에 적합한 길이)
        secretKey = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded());
        JwtProperties jwtProperties = new JwtProperties(
                secretKey,          // secretKey
                3600000L,           // accessTokenExpiryMs (1시간)
                604800000L          // refreshTokenExpiryMs (7일)
        );
        // 2. Provider 생성 및 초기화 (생성자 주입 -> init 수동 호출)
        jwtProvider = new JwtProvider(jwtProperties);
        jwtProvider.init(); // @PostConstruct 역할을 수동으로 수행
    }

    @Test
    @DisplayName("Access Token 발급 시 Claims 정보가 올바르게 들어간다")
    void generateAccessToken_success() {
        // given
        Long memberId = 1L;
        String email = "test@example.com";
        MemberRole role = MemberRole.USER;
        LoginType loginType = LoginType.GOOGLE;

        // Member Mock 동작 정의
        given(member.getId()).willReturn(memberId);
        given(member.getEmail()).willReturn(new Email(email)); // Email은 값 객체라 실제 객체 사용 가능
        given(member.getRole()).willReturn(role);
        given(member.getLoginType()).willReturn(loginType);

        // when
        String token = jwtProvider.generateAccessToken(member);

        // then
        assertThat(token).isNotNull();

        // 토큰 복호화 및 검증
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
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
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
        // Refresh Token에는 보통 상세 개인정보를 넣지 않으므로 email 등은 확인하지 않음
    }

    @Test
    @DisplayName("토큰에서 Member ID를 정상적으로 추출한다")
    void extractMemberId_success() {
        // given
        Long memberId = 100L;
        given(member.getId()).willReturn(memberId);
        // 테스트 편의를 위해 generateRefreshToken 사용하여 토큰 생성 (AccessToken 생성 시 설정이 더 많아서)
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
        String tamperedToken = validToken + "d"; // 토큰 내용 임의 변경

        // when
        boolean isValid = jwtProvider.isTokenValid(tamperedToken);

        // then
        assertThat(isValid).isFalse();
    }
}
