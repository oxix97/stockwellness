package org.stockwellness.global.security.jwt;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.error.ErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException {
        try {
            filterChain.doFilter(request, response);  // JwtAuthenticationFilter로 넘김
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰 접근 - URI: {}, Remote IP: {}", request.getRequestURI(), request.getRemoteAddr());
            setErrorResponse(response, ErrorCode.EXPIRED_JWT);
        } catch (JwtException e) {  // Malformed, Signature, Unsupported 등
            log.warn("유효하지 않은 JWT 토큰 접근 - URI: {}, Cause: {}", request.getRequestURI(), e.getClass().getSimpleName());
            setErrorResponse(response, ErrorCode.INVALID_JWT);
        } catch (Exception e) {     // 예상치 못한 다른 예외도 여기서 잡아서 401로 통일 (필요 시)
            log.error("JWT 필터 체인 중 예상치 못한 예외 발생 - URI: {}", request.getRequestURI(), e);
            setErrorResponse(response, ErrorCode.UNAUTHORIZED);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode, null);
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
    }
}
