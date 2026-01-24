package org.stockwellness.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.stockwellness.adapter.out.security.jwt.JwtProvider;
import org.stockwellness.global.security.CustomUserDetailsService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    JwtProvider jwtProvider;
    @Mock
    FilterChain filterChain;
    @Mock
    CustomUserDetailsService userDetailsService;
    @Mock
    UserDetails userDetails;
    @InjectMocks
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("유효한 토큰이 제공되면 SecurityContext에 인증 객체가 저장된다")
        void valid_token_sets_authentication() throws ServletException, IOException {
            // given
            String token = "valid.jwt.token";
            given(jwtProvider.validateAndGetId(token)).willReturn(1L);
            given(userDetailsService.loadUserByUsername("1")).willReturn(userDetails);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(userDetailsService).loadUserByUsername("1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("계정이 비활성화 상태라면(Service에서 예외 발생) 인증되지 않는다")
        void doFilterInternal_fail_disabled() throws ServletException, IOException {
            // given
            String token = "valid.token";
            given(jwtProvider.validateAndGetId(token)).willReturn(1L);

            // Service가 DisabledException을 던지도록 설정
            given(userDetailsService.loadUserByUsername("1"))
                    .willThrow(new DisabledException("비활성화된 계정"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when & then
            // 필터가 예외를 다시 던지므로 검증
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(DisabledException.class);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("실패 케이스 (세분화)")
    class Failure {
        @Test
        @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 진행한다")
        void no_header() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest(); // 헤더 없음
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtProvider, never()).validateAndGetId(anyString()); // 검증 시도조차 안 함
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 접두사가 없으면 인증 없이 다음 필터로 진행한다")
        void invalid_prefix() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic some_token"); // Wrong Prefix
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("토큰 검증(validate)이 실패하면 인증 객체 없이 다음 필터로 진행한다")
        void invalid_token_validation() throws ServletException, IOException {
            // given
            String token = "invalid.token";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // 예외를 던지도록 설정 (필터 내부에서 catch 후 rethrow)
            given(jwtProvider.validateAndGetId(token)).willThrow(new io.jsonwebtoken.JwtException("Invalid Token"));

            // when & then
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(request, response);
        }
    }
}
