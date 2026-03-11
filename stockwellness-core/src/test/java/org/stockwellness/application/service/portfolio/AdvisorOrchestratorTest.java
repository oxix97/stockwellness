package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.portfolio.AiAdvisorPort;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.portfolio.SaveAdvisorPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvisorOrchestrator 단위 테스트")
class AdvisorOrchestratorTest {

    @InjectMocks
    private AdvisorOrchestrator orchestrator;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private AdvisorAiDataLoader dataLoader;

    @Mock
    private AiAdvisorPort aiAdvisorPort;

    @Mock
    private SaveAdvisorPort saveAdvisorPort;

    @Test
    @DisplayName("포트폴리오 조언 생성 프로세스가 정상적으로 수행된다")
    void generateAndSaveAdvice_success() {
        // given
        Long portfolioId = 1L;
        Portfolio portfolio = Portfolio.create(1L, "테스트", "");
        given(portfolioPort.findById(portfolioId)).willReturn(Optional.of(portfolio));
        
        AdvisorAiContext context = mock(AdvisorAiContext.class);
        given(dataLoader.loadContext(portfolioId)).willReturn(context);
        
        AiAdvisorPort.AdvisorAiResult aiResult = new AiAdvisorPort.AdvisorAiResult(
                "", "", "", "내용", AdviceAction.REBALANCE
        );
        given(aiAdvisorPort.getRebalancingAdvice(context)).willReturn(aiResult);

        // when
        orchestrator.generateAndSaveAdvice(portfolioId);

        // then
        verify(saveAdvisorPort, times(1)).saveReport(any(AdvisorReport.class));
    }

    @Test
    @DisplayName("모든 포트폴리오에 대해 조언 생성 루프가 실행된다")
    void runAllPortfolios_success() {
        // given
        Portfolio p1 = mock(Portfolio.class);
        Portfolio p2 = mock(Portfolio.class);
        given(p1.getId()).willReturn(1L);
        given(p2.getId()).willReturn(2L);
        given(portfolioPort.loadAllPortfolios(null)).willReturn(List.of(p1, p2));
        
        given(portfolioPort.findById(1L)).willReturn(Optional.of(p1));
        given(portfolioPort.findById(2L)).willReturn(Optional.of(p2));
        
        // when
        orchestrator.runAllPortfolios();

        // then
        verify(aiAdvisorPort, times(2)).getRebalancingAdvice(any());
    }
}
