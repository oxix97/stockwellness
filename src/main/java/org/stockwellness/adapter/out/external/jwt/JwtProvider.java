package org.stockwellness.adapter.out.external.jwt;// adapter/out/external/jwt/JwtTokenProvider.java (이름 JwtProvider로 추천)

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.GenerateTokenPort;
import org.stockwellness.application.port.out.ValidateTokenPort;
import org.stockwellness.domain.member.Member;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider implements GenerateTokenPort, ValidateTokenPort {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secretKey());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);  // 최신 방식 (알고리즘 자동 선택)
    }

    @Override
    public String generateAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessTokenExpiryMs());

        return Jwts.builder()
                .subject(member.getId().toString())  // memberId
                .claim("email", member.getEmail().address())
                .claim("loginType", member.getLoginType())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)  // 0.12.x 최신: Key만 넣으면 알고리즘 자동
                .compact();
    }

    @Override
    public String generateRefreshToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTokenExpiryMs());

        return Jwts.builder()
                .subject(member.getId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 추출 메서드들 (필터에서 사용)
    @Override
    public Long extractMemberId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    // 기타 extract 메서드 추가 가능

    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}