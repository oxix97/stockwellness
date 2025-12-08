package org.stockwellness.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stockwellness.adapter.out.external.jwt.JwtProvider;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRepository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtProvider.isTokenValid(jwt)) {
                Long memberId = jwtProvider.extractMemberId(jwt);

                // DB에서 최신 Member 정보 로드 (role 변경 등 반영을 위해 캐시하지 않음)
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

                if (member.isActive()) {
                    setAuthenticationToContext(member, jwt, request);
                    log.debug("JWT 인증 성공 - memberId: {}, email: {}", memberId, member.getEmail().address());
                } else {
                    log.warn("비활성화된 회원 JWT 접근 시도 - memberId: {}", memberId);
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생 - URI: {}", request.getRequestURI(), e);
            // JwtExceptionFilter에서 ProblemDetail로 변환되므로 여기서는 그냥 넘김
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void setAuthenticationToContext(Member member, String jwt, HttpServletRequest request) {
        // 1. 권한 목록 생성 (ROLE_ 접두사 자동 추가)
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(member.getRole().name())
        );

        // 2. UserDetails 대신 Member 자체를 principal로 사용 가능하지만,
        //    Spring Security 표준에 맞게 UserDetails 구현체 사용
        UserDetails userDetails = User.withUsername(member.getId().toString())
                .password("")  // password 불필요
                .authorities(authorities)
                .build();

        // 3. Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, jwt, authorities);

        // 4. 요청 상세 정보 추가 (IP, Session ID 등)
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 5. SecurityContext에 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}