package org.stockwellness.global.security.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginFailureHandlerTest {

    private final OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler();

    @Test
    @DisplayName("OAuth2 로그인 실패 시 프론트 callback으로 errorCode만 전달한다")
    void onAuthenticationFailure_redirects_with_error_code() throws IOException, ServletException {
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", "http://localhost:5173/auth/callback");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new OAuth2AuthenticationException("oauth2_login_failed");

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback?errorCode=A007");
    }
}
