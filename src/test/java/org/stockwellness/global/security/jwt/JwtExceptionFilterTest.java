package org.stockwellness.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.stockwellness.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class JwtExceptionFilterTest {

    @InjectMocks
    JwtExceptionFilter jwtExceptionFilter;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @Spy
    ObjectMapper objectMapper;

    StringWriter responseBodyWriter;

    @BeforeEach
    void setUp() throws IOException {
        responseBodyWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseBodyWriter);

        lenient().when(response.getWriter()).thenReturn(writer);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정상 흐름: 예외가 없으면 다음 필터로 진행한다")
        void doFilterInternal_Success() throws ServletException, IOException {
            // when
            jwtExceptionFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain, times(1)).doFilter(request, response);
            // 정상 흐름에서는 response에 에러를 쓰지 않으므로 status 설정 등이 없어야 함
            verify(response, times(0)).setStatus(any(Integer.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("예외 발생: ExpiredJwtException 발생 시 EXPIRED_JWT 에러 반환")
        void doFilterInternal_ExpiredJwtException() throws ServletException, IOException {
            // given
            willThrow(new ExpiredJwtException(null, null, "Token expired"))
                    .given(filterChain).doFilter(request, response);

            // when
            jwtExceptionFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(ErrorCode.EXPIRED_JWT.getStatusCode());
            verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            // JSON 응답 검증
            String responseBody = responseBodyWriter.toString();
            assertThat(responseBody).contains("EXPIRED_JWT");
            assertThat(responseBody).contains(ErrorCode.EXPIRED_JWT.getMessage());
        }

        @Test
        @DisplayName("예외 발생: MalformedJwtException 발생 시 INVALID_JWT 에러 반환")
        void doFilterInternal_JwtException() throws ServletException, IOException {
            // given
            // MalformedJwtException은 JwtException을 상속받음
            willThrow(new MalformedJwtException("Malformed token"))
                    .given(filterChain).doFilter(request, response);

            // when
            jwtExceptionFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(ErrorCode.INVALID_JWT.getStatusCode());
            verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            String responseBody = responseBodyWriter.toString();
            assertThat(responseBody).contains("INVALID_JWT");
        }

        @Test
        @DisplayName("예외 발생: 알 수 없는 예외 발생 시 UNAUTHORIZED 에러 반환")
        void doFilterInternal_GeneralException() throws ServletException, IOException {
            // given
            willThrow(new RuntimeException("Unexpected error"))
                    .given(filterChain).doFilter(request, response);

            // when
            jwtExceptionFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(ErrorCode.UNAUTHORIZED.getStatusCode());

            String responseBody = responseBodyWriter.toString();
            assertThat(responseBody).contains("UNAUTHORIZED");
        }
    }


}