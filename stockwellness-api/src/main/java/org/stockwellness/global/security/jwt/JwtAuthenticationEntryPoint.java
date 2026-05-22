package org.stockwellness.global.security.jwt;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper; // 주입 받아 사용

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String requestURI = request.getRequestURI();
        ErrorCode errorCode = determineErrorCode(requestURI);
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode, null);

        response.setStatus(errorCode.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private ErrorCode determineErrorCode(String requestURI) {
        if (requestURI.startsWith("/api/v1/portfolios") ||
            requestURI.startsWith("/api/v1/watchlist") ||
            requestURI.startsWith("/api/v1/members")) {
            return ErrorCode.REQUIRE_SIGNUP;
        }
        return ErrorCode.UNAUTHORIZED;
    }
}
