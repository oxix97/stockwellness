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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAccessDeniedHandlerTest {

    @InjectMocks
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    private StringWriter responseBodyWriter;

    @BeforeEach
    void setUp() throws IOException {
        // Response Writer 캡처 설정
        responseBodyWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseBodyWriter);

        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    @DisplayName("권한 부족 시 403 Forbidden 상태와 JSON 형식의 에러 메시지를 반환한다")
    void handle_ShouldReturnForbiddenJson() throws IOException {
        // given
        AccessDeniedException exception = new AccessDeniedException("Access Denied");

        // when
        jwtAccessDeniedHandler.handle(request, response, exception);

        // then
        // 1. 상태 코드 검증 (403)
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());

        // 2. 헤더 검증
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");

        // 3. JSON 응답 본문 검증
        String responseBody = responseBodyWriter.toString();

        assertThat(responseBody)
                .contains("\"title\":\"FORBIDDEN\"")
                .contains("\"detail\":\"접근 권한이 없습니다.\"")
                .contains("\"errorCode\":\"FORBIDDEN\"")
                .startsWith("{").endsWith("}");
    }
}