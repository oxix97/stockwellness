package org.stockwellness.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.global.security.MemberPrincipal;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthUseCase authUseCase;

    @Value("${app.frontend-redirect-url:http://localhost:3000/oauth/callback}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        // 1. LoginCommand를 생성하여 AuthService 호출 (가입/로그인 통합 처리)
        LoginCommand command = new LoginCommand(
                principal.email(),
                principal.nickname(),
                principal.loginType()
        );
        LoginResult loginResult = authUseCase.login(command);

        // 2. 프론트엔드 리다이렉트 (토큰 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", loginResult.accessToken())
                .queryParam("refreshToken", loginResult.refreshToken())
                .build().toUriString();

        log.info("OAuth2 Login Success for member: {}. Redirecting to {}", loginResult.memberId(), targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}