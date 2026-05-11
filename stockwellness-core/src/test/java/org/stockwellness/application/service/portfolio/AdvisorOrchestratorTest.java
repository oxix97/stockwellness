package org.stockwellness.application.service.portfolio;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.portfolio.SaveAdvisorPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
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
    private AiAdviceProviderPort aiAdviceProviderPort;

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
        
        AiAdviceProviderPort.AdvisorAiResult aiResult = new AiAdviceProviderPort.AdvisorAiResult(
                "", "", "", "내용", AdviceAction.REBALANCE
        );
        given(aiAdviceProviderPort.getRebalancingAdvice(context)).willReturn(aiResult);

        // when
        orchestrator.generateAndSaveAdvice(portfolioId);

        // then
        verify(saveAdvisorPort, times(1)).saveReport(any(AdvisorReport.class));
    }

    @Test
    @DisplayName("모든 포트폴리오에 대해 조언 생성 루프가 실행된다")
    void runAllPortfolios_success() {
        // given
        Long id1 = 1L;
        Long id2 = 2L;
        
        given(portfolioPort.findAllIds(0, 100)).willReturn(List.of(id1, id2));
        given(portfolioPort.findAllIds(100, 100)).willReturn(List.of());
        
        Portfolio p1 = mock(Portfolio.class);
        Portfolio p2 = mock(Portfolio.class);
        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        
        given(portfolioPort.findById(id1)).willReturn(Optional.of(p1));
        given(portfolioPort.findById(id2)).willReturn(Optional.of(p2));

        // 데이터 로더와 조언 제공자 Stubbing (generateAndSaveAdvice 내부용)
        lenient().when(dataLoader.loadContext(anyLong())).thenReturn(mock(AdvisorAiContext.class));
        lenient().when(aiAdviceProviderPort.getRebalancingAdvice(any())).thenReturn(new AiAdviceProviderPort.AdvisorAiResult("", "", "", "내용", AdviceAction.REBALANCE));
        
        // when
        orchestrator.runAllPortfolios();

        // then
        verify(portfolioPort, times(1)).findById(id1);
        verify(portfolioPort, times(1)).findById(id2);
        verify(aiAdviceProviderPort, times(2)).getRebalancingAdvice(any());
    }
}
