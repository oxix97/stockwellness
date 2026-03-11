package org.stockwellness.integration.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.security.jwt.JwtProvider;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.service.portfolio.AdvisorOrchestrator;
import org.stockwellness.config.CoreRedisConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.global.security.CustomUserDetailsService;
import org.stockwellness.global.security.MemberPrincipal;
import org.stockwellness.support.RestDocsSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("AI 어드바이저 E2E 통합 테스트")
class AiAdvisorIntegrationTest extends RestDocsSupport {

    @Autowired
    private AdvisorOrchestrator advisorOrchestrator;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CoreRedisConfig coreRedisConfig;

    @Test
    @DisplayName("포트폴리오 조언 생성부터 API 조회까지의 전체 흐름을 검증한다")
    @org.stockwellness.support.annotation.MockMember(id = 1L)
    void advisor_e2e_flow_success() throws Exception {
        // given
        Long memberId = 1L;
        Member member = Member.register("test@test.com", "테스트유저", LoginType.KAKAO);
        memberRepository.save(member);

        String validToken = "valid-token";
        
        // Mock jwt behavior explicitly for testing without true generation
        given(jwtProvider.validateAndGetId(anyString())).willReturn(memberId);
        given(jwtProvider.isTokenValid(anyString())).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(memberId.toString()))
                .willReturn(MemberPrincipal.of(member));

        Portfolio portfolio = Portfolio.create(memberId, "통합 테스트 포트폴리오", "설명");
        portfolioRepository.save(portfolio);
        Long portfolioId = portfolio.getId();

        AiAdviceProviderPort.AdvisorAiResult aiResult = new AiAdviceProviderPort.AdvisorAiResult(
                "Target OK", "Technical Good", "Low Risk", "성공적인 통합 테스트 조언", AdviceAction.RISK_MANAGEMENT
        );
        
        // Use the inherited @MockBean aiAdviceProviderPort
        given(aiAdviceProviderPort.getRebalancingAdvice(any())).willReturn(aiResult);

        // when
        advisorOrchestrator.generateAndSaveAdvice(portfolioId);

        // then
        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/advice/latest", portfolioId)
                        .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("성공적인 통합 테스트 조언"))
                .andExpect(jsonPath("$.action").value("RISK_MANAGEMENT"));
    }
}

