package org.stockwellness.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class KakaoOAuth2ConfigTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("Kakao OAuth2 클라이언트 설정이 로드되어야 한다")
    void kakaoClientRegistrationShouldBeLoaded() {
        ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");
        assertThat(kakaoRegistration).isNotNull();
        assertThat(kakaoRegistration.getClientName()).isEqualTo("Kakao");
        assertThat(kakaoRegistration.getScopes()).contains("profile_nickname", "account_email");
    }
}
