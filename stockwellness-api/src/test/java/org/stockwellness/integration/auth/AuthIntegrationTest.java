package org.stockwellness.integration.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.application.port.in.auth.dto.LoginRequest;
import org.stockwellness.application.port.in.auth.dto.ReissueRequest;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.fixture.AuthFixture;
import org.stockwellness.fixture.MemberFixture;
import org.stockwellness.integration.common.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth API E2E 통합 테스트")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 가입 및 로그인 흐름이 정상적으로 동작한다")
    void register_and_login_success() throws Exception {
        // given
        String email = "newuser@example.com";
        LoginRequest request = new LoginRequest(email, "New User", LoginType.KAKAO);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.email").value(email));

        // DB 저장 확인
        assertThat(memberRepository.findByEmail(new org.stockwellness.domain.shared.Email(email))).isPresent();
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 재발급받는다")
    void reissue_token_success() throws Exception {
        // given: 로그인하여 리프레시 토큰 확보
        LoginRequest loginRequest = AuthFixture.createLoginRequest();
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andReturn().getResponse().getContentAsString();
        
        var jsonNode = objectMapper.readTree(loginResponse);
        if (jsonNode.get("data") == null || jsonNode.get("data").get("refreshToken") == null) {
            throw new RuntimeException("로그인 실패 또는 토큰 누락: " + loginResponse);
        }
        
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();
        Long memberId = jsonNode.get("data").get("memberId").asLong();
        ReissueRequest reissueRequest = new ReissueRequest(refreshToken);

        given(refreshTokenPort.findByMemberId(org.mockito.ArgumentMatchers.any())).willReturn(
                org.stockwellness.domain.auth.RefreshToken.create(memberId, refreshToken, org.stockwellness.global.util.DateUtil.now().plusDays(1))
        );

        // when & then
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }
}
