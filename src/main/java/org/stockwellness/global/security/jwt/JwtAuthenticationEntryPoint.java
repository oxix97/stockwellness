package org.stockwellness.global.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.stockwellness.global.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper; // 주입 받아 사용

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                ErrorCode.UNAUTHORIZED.getStatus(),
                ErrorCode.UNAUTHORIZED.getMessage()
        );
        body.setTitle(ErrorCode.UNAUTHORIZED.name());
        body.setProperty("errorCode", ErrorCode.UNAUTHORIZED.name());

        response.setStatus(ErrorCode.UNAUTHORIZED.getStatusCode());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}