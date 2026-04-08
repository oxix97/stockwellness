package org.stockwellness.global.security.handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.global.security.MemberPrincipal;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OAuth2LoginSuccessHandlerTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(authUseCase);

    @Test
    @DisplayName("OAuth2 로그인 성공 시 accessToken과 refreshToken으로 callback 리다이렉트한다")
    void onAuthenticationSuccess_redirects_with_tokens() throws IOException, ServletException {
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", "http://localhost:5173/auth/callback");

        MemberPrincipal principal = new MemberPrincipal(
                1L,
                "user@example.com",
                "tester",
                LoginType.GOOGLE,
                MemberRole.USER,
                Collections.emptyList(),
                Collections.emptyMap()
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        given(authUseCase.login(any())).willReturn(
                new LoginResult("access-token", "refresh-token", 1L, "user@example.com", "tester", LocalDate.of(2026, 4, 7))
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback?accessToken=access-token&refreshToken=refresh-token");
    }
}
