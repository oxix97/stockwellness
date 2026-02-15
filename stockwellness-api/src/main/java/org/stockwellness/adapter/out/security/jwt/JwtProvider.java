package org.stockwellness.adapter.out.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.auth.GenerateTokenPort;
import org.stockwellness.application.port.out.auth.ValidateTokenPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider implements GenerateTokenPort, ValidateTokenPort {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secretKey());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(Member member) {
        return generateAccessToken(member.getId(), member.getEmail().getAddress(), member.getLoginType(), member.getRole());
    }

    @Override
    public String generateRefreshToken(Member member) {
        return generateRefreshToken(member.getId());
    }

    @Override
    public String generateAccessToken(Long id, String email, LoginType loginType, MemberRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessTokenExpiryMs());

        return Jwts.builder()
                .subject(id.toString())
                .claim("email", email)
                .claim("loginType", loginType)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long id) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTokenExpiryMs());

        return Jwts.builder()
                .subject(id.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰 검증 및 MemberId 추출을 한 번에 수행 (성능 최적화)
     * @return memberId (유효하지 않으면 예외 발생)
     */
    @Override
    public Long validateAndGetId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return Long.valueOf(claims.getSubject());
    }
    
    // extractMemberId (RefreshTokenPort 등 다른 곳에서 쓸 수 있으므로 public 유지 또는 private로 변경 검토)
    // 현재는 ValidateTokenPort를 통해서만 호출되므로 제거해도 됨. 
    // 하지만 AuthService.reissue에서 extractMemberId를 쓰고 있음. -> AuthService 수정 필요.
    
    public Long extractMemberId(String token) {
        return validateAndGetId(token);
    }
    
    public boolean isTokenValid(String token) {
        try {
            validateAndGetId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
