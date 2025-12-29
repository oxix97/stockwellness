package org.stockwellness.global.security.jwt;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.stockwellness.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @InjectMocks
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private StringWriter responseBodyWriter;

    @BeforeEach
    void setUp() throws IOException {
        // Response Writer 캡처 설정
        responseBodyWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseBodyWriter);

        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    @DisplayName("인증 실패 시 401 Unauthorized 상태와 JSON 에러 응답을 반환한다")
    void commence_ShouldReturnUnauthorizedResponse() throws IOException {
        // given
        // AuthenticationException은 추상 클래스이므로 익명 클래스나 구체 클래스로 생성
        AuthenticationException authException = new AuthenticationException("Authentication failed") {};

        // when
        jwtAuthenticationEntryPoint.commence(request, response, authException);

        // then
        // 1. 상태 코드 검증 (401 가정)
        verify(response).setStatus(ErrorCode.UNAUTHORIZED.getStatusCode());

        // 2. 헤더 검증
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");

        // 3. 응답 본문 JSON 검증
        String responseBody = responseBodyWriter.toString();

        // ErrorCode 내용이 JSON에 잘 들어갔는지 확인
        assertThat(responseBody).contains(ErrorCode.UNAUTHORIZED.name()); // "UNAUTHORIZED"
        assertThat(responseBody).contains(ErrorCode.UNAUTHORIZED.getMessage());

        // 커스텀 프로퍼티 확인 ("errorCode")
        assertThat(responseBody).contains("\"errorCode\":\"" + ErrorCode.UNAUTHORIZED.name() + "\"");

        // 유효한 JSON 형식인지 시작/끝 확인
        assertThat(responseBody).startsWith("{").endsWith("}");
    }
}