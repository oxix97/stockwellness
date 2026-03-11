package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.port.out.portfolio.AiAdvisorPort;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAdvisorService 단위 테스트")
class AiAdvisorServiceTest {

    @InjectMocks
    private AiAdvisorService aiAdvisorService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private AdvisorAiDataLoader dataLoader;

    @Mock
    private AiAdvisorPort aiAdvisorPort;

    @Test
    @DisplayName("AI 리밸런싱 조언을 성공적으로 생성한다")
    void getAdvice_success() {
        // given
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "테스트 포트폴리오", "");
        
        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));
        
        AdvisorAiContext context = new AdvisorAiContext(portfolio.getName(), List.of(), List.of());
        given(dataLoader.loadContext(portfolioId)).willReturn(context);
        
        AiAdvisorPort.AdvisorAiResult aiResult = new AiAdvisorPort.AdvisorAiResult(
                "Target OK", "Technical Good", "Low Risk", "조언 내용", AdviceAction.REBALANCE
        );
        given(aiAdvisorPort.getRebalancingAdvice(context)).willReturn(aiResult);

        // when
        AdviceResponse response = aiAdvisorService.getAdvice(memberId, portfolioId);

        // then
        assertThat(response.content()).isEqualTo("조언 내용");
        assertThat(response.action()).isEqualTo(AdviceAction.REBALANCE);
        assertThat(response.createdAt()).isNotNull();
    }
}
