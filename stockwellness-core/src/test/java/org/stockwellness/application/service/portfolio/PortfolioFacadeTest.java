package org.stockwellness.application.service.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.in.portfolio.AiAdvisorUseCase;
import org.stockwellness.application.port.in.portfolio.DiagnosePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.LoadPortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.ManagePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.PortfolioAnalysisUseCase;
import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.result.*;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioFacade 단위 테스트")
class PortfolioFacadeTest {

    @InjectMocks
    private PortfolioFacade portfolioFacade;

    @Mock
    private ManagePortfolioUseCase managePortfolioUseCase;

    @Mock
    private LoadPortfolioUseCase loadPortfolioUseCase;

    @Mock
    private PortfolioAnalysisUseCase portfolioAnalysisUseCase;

    @Mock
    private DiagnosePortfolioUseCase diagnosePortfolioUseCase;

    @Mock
    private AiAdvisorUseCase aiAdvisorUseCase;

    private static final Long MEMBER_ID = 1L;
    private static final Long PORTFOLIO_ID = 100L;

    @Nested
    @DisplayName("기본 위임 테스트")
    class DelegationTests {

        @Test
        @DisplayName("포트폴리오 관리 요청은 ManagePortfolioUseCase로 위임한다")
        void delegateManageUseCase() {
            CreatePortfolioCommand createCommand = mock(CreatePortfolioCommand.class);
            UpdatePortfolioCommand updateCommand = mock(UpdatePortfolioCommand.class);
            given(managePortfolioUseCase.createPortfolio(createCommand)).willReturn(PORTFOLIO_ID);

            Long createdId = portfolioFacade.createPortfolio(createCommand);
            portfolioFacade.updatePortfolio(updateCommand);
            portfolioFacade.deletePortfolio(MEMBER_ID, PORTFOLIO_ID);

            assertThat(createdId).isEqualTo(PORTFOLIO_ID);
            verify(managePortfolioUseCase).createPortfolio(createCommand);
            verify(managePortfolioUseCase).updatePortfolio(updateCommand);
            verify(managePortfolioUseCase).deletePortfolio(MEMBER_ID, PORTFOLIO_ID);
        }

        @Test
        @DisplayName("포트폴리오 조회 요청은 LoadPortfolioUseCase로 위임한다")
        void delegateLoadUseCase() {
            PortfolioResponse portfolio = mock(PortfolioResponse.class);
            List<PortfolioResponse> portfolios = List.of(portfolio);
            given(loadPortfolioUseCase.getMyPortfolios(MEMBER_ID)).willReturn(portfolios);
            given(loadPortfolioUseCase.getPortfolio(MEMBER_ID, PORTFOLIO_ID)).willReturn(portfolio);

            assertThat(portfolioFacade.getMyPortfolios(MEMBER_ID)).isSameAs(portfolios);
            assertThat(portfolioFacade.getPortfolio(MEMBER_ID, PORTFOLIO_ID)).isSameAs(portfolio);
            verify(loadPortfolioUseCase).getMyPortfolios(MEMBER_ID);
            verify(loadPortfolioUseCase).getPortfolio(MEMBER_ID, PORTFOLIO_ID);
        }

        @Test
        @DisplayName("포트폴리오 분석 요청은 PortfolioAnalysisUseCase로 위임한다")
        void delegateAnalysisUseCase() {
            PortfolioValuationResult valuation = mock(PortfolioValuationResult.class);
            PortfolioDiversificationResult diversification = mock(PortfolioDiversificationResult.class);
            PortfolioRebalancingResult rebalancing = mock(PortfolioRebalancingResult.class);
            BacktestPortfolioCommand backtestCommand = mock(BacktestPortfolioCommand.class);
            BacktestResult backtestResult = mock(BacktestResult.class);
            Map<String, Map<String, BigDecimal>> correlation = Map.of("005930", Map.of("000660", BigDecimal.ONE));
            PortfolioInceptionPerformanceResult performance = mock(PortfolioInceptionPerformanceResult.class);
            PortfolioInceptionChartResult chart = mock(PortfolioInceptionChartResult.class);

            given(portfolioAnalysisUseCase.getValuation(MEMBER_ID, PORTFOLIO_ID)).willReturn(valuation);
            given(portfolioAnalysisUseCase.getDiversification(MEMBER_ID, PORTFOLIO_ID)).willReturn(diversification);
            given(portfolioAnalysisUseCase.getRebalancingGuide(MEMBER_ID, PORTFOLIO_ID)).willReturn(rebalancing);
            given(portfolioAnalysisUseCase.runBacktest(backtestCommand)).willReturn(backtestResult);
            given(portfolioAnalysisUseCase.getCorrelationMatrix(MEMBER_ID, PORTFOLIO_ID)).willReturn(correlation);
            given(portfolioAnalysisUseCase.getPerformanceSinceInception(MEMBER_ID, PORTFOLIO_ID)).willReturn(performance);
            given(portfolioAnalysisUseCase.getInceptionChart(MEMBER_ID, PORTFOLIO_ID)).willReturn(chart);

            assertThat(portfolioFacade.getValuation(MEMBER_ID, PORTFOLIO_ID)).isSameAs(valuation);
            assertThat(portfolioFacade.getDiversification(MEMBER_ID, PORTFOLIO_ID)).isSameAs(diversification);
            assertThat(portfolioFacade.getRebalancingGuide(MEMBER_ID, PORTFOLIO_ID)).isSameAs(rebalancing);
            assertThat(portfolioFacade.runBacktest(backtestCommand)).isSameAs(backtestResult);
            assertThat(portfolioFacade.getCorrelationMatrix(MEMBER_ID, PORTFOLIO_ID)).isSameAs(correlation);
            assertThat(portfolioFacade.getPerformanceSinceInception(MEMBER_ID, PORTFOLIO_ID)).isSameAs(performance);
            assertThat(portfolioFacade.getInceptionChart(MEMBER_ID, PORTFOLIO_ID)).isSameAs(chart);
        }
    }

    @Nested
    @DisplayName("포트폴리오 분석 및 요약 테스트")
    class AnalysisTests {

        @Test
        @DisplayName("분석 요약 조회 시 날짜가 없으면 종료일은 오늘, 시작일은 12개월 전으로 보정한다")
        void getAnalysisSummary_DefaultDateRange() {
            PortfolioAnalysisSummaryResult summary = mock(PortfolioAnalysisSummaryResult.class);
            given(portfolioAnalysisUseCase.getAnalysisSummary(
                    eq(MEMBER_ID),
                    eq(PORTFOLIO_ID),
                    any(LocalDate.class),
                    any(LocalDate.class)
            )).willReturn(summary);

            LocalDate beforeCall = LocalDate.now();
            PortfolioAnalysisSummaryResult result = portfolioFacade.getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, null, null);
            LocalDate afterCall = LocalDate.now();

            ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(portfolioAnalysisUseCase).getAnalysisSummary(
                    eq(MEMBER_ID),
                    eq(PORTFOLIO_ID),
                    startCaptor.capture(),
                    endCaptor.capture()
            );
            assertThat(result).isSameAs(summary);
            assertThat(endCaptor.getValue()).isBetween(beforeCall, afterCall);
            assertThat(startCaptor.getValue()).isEqualTo(endCaptor.getValue().minusMonths(12));
        }

        @Test
        @DisplayName("시작일만 있는 경우 종료일은 오늘로 보정한다")
        void getAnalysisSummary_OnlyStartDate() {
            PortfolioAnalysisSummaryResult summary = mock(PortfolioAnalysisSummaryResult.class);
            LocalDate startDate = LocalDate.of(2023, 1, 1);
            given(portfolioAnalysisUseCase.getAnalysisSummary(
                    eq(MEMBER_ID),
                    eq(PORTFOLIO_ID),
                    eq(startDate),
                    any(LocalDate.class)
            )).willReturn(summary);

            LocalDate beforeCall = LocalDate.now();
            PortfolioAnalysisSummaryResult result = portfolioFacade.getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, startDate, null);
            LocalDate afterCall = LocalDate.now();

            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(portfolioAnalysisUseCase).getAnalysisSummary(
                    eq(MEMBER_ID),
                    eq(PORTFOLIO_ID),
                    eq(startDate),
                    endCaptor.capture()
            );
            assertThat(result).isSameAs(summary);
            assertThat(endCaptor.getValue()).isBetween(beforeCall, afterCall);
        }

        @Test
        @DisplayName("종료일만 있는 경우 시작일은 종료일 기준 12개월 전으로 보정한다")
        void getAnalysisSummary_OnlyEndDate() {
            PortfolioAnalysisSummaryResult summary = mock(PortfolioAnalysisSummaryResult.class);
            LocalDate endDate = LocalDate.of(2023, 12, 31);
            LocalDate expectedStartDate = endDate.minusMonths(12);
            given(portfolioAnalysisUseCase.getAnalysisSummary(
                    eq(MEMBER_ID),
                    eq(PORTFOLIO_ID),
                    eq(expectedStartDate),
                    eq(endDate)
            )).willReturn(summary);

            PortfolioAnalysisSummaryResult result = portfolioFacade.getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, null, endDate);

            assertThat(result).isSameAs(summary);
            verify(portfolioAnalysisUseCase).getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, expectedStartDate, endDate);
        }
    }

    @Nested
    @DisplayName("AI 기능 토글 테스트")
    class AiToggleTests {

        @Test
        @DisplayName("AI가 비활성화되면 진단과 조언은 Mock 응답을 반환하고 UseCase를 호출하지 않는다")
        void returnMockResponsesWhenAiDisabled() {
            ReflectionTestUtils.setField(portfolioFacade, "aiEnabled", false);

            PortfolioHealthResult healthResult = portfolioFacade.diagnosePortfolio(MEMBER_ID, PORTFOLIO_ID);
            AdviceResponse latestAdvice = portfolioFacade.getLatestAdvice(MEMBER_ID, PORTFOLIO_ID);
            AdviceResponse newAdvice = portfolioFacade.getNewAdvice(MEMBER_ID, PORTFOLIO_ID);

            assertThat(healthResult.summary()).contains("Mock AI 진단");
            assertThat(latestAdvice.content()).contains("Mock AI 조언");
            assertThat(newAdvice.content()).contains("Mock AI 조언");
            verifyNoInteractions(diagnosePortfolioUseCase, aiAdvisorUseCase);
        }

        @Test
        @DisplayName("AI가 활성화되면 진단과 조언 요청은 각 UseCase로 위임한다")
        void delegateAiUseCasesWhenAiEnabled() {
            ReflectionTestUtils.setField(portfolioFacade, "aiEnabled", true);
            PortfolioHealthResult healthResult = PortfolioHealthResult.mock();
            AdviceResponse latestAdvice = AdviceResponse.mock();
            AdviceResponse newAdvice = AdviceResponse.mock();
            given(diagnosePortfolioUseCase.diagnosePortfolio(MEMBER_ID, PORTFOLIO_ID)).willReturn(healthResult);
            given(aiAdvisorUseCase.getLatestAdvice(MEMBER_ID, PORTFOLIO_ID)).willReturn(latestAdvice);
            given(aiAdvisorUseCase.getNewAdvice(MEMBER_ID, PORTFOLIO_ID)).willReturn(newAdvice);

            assertThat(portfolioFacade.diagnosePortfolio(MEMBER_ID, PORTFOLIO_ID)).isSameAs(healthResult);
            assertThat(portfolioFacade.getLatestAdvice(MEMBER_ID, PORTFOLIO_ID)).isSameAs(latestAdvice);
            assertThat(portfolioFacade.getNewAdvice(MEMBER_ID, PORTFOLIO_ID)).isSameAs(newAdvice);
            verify(diagnosePortfolioUseCase).diagnosePortfolio(MEMBER_ID, PORTFOLIO_ID);
            verify(aiAdvisorUseCase).getLatestAdvice(MEMBER_ID, PORTFOLIO_ID);
            verify(aiAdvisorUseCase).getNewAdvice(MEMBER_ID, PORTFOLIO_ID);
            verify(aiAdvisorUseCase, never()).generateBacktestAdvice(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("예외 전파 테스트")
    class ExceptionTests {

        @Test
        @DisplayName("UseCase에서 발생한 예외는 Facade에서 그대로 전파되어야 한다")
        void propagateExceptionFromUseCase() {
            // given
            CreatePortfolioCommand command = mock(CreatePortfolioCommand.class);
            String errorMessage = "Domain Error";
            given(managePortfolioUseCase.createPortfolio(any())).willThrow(new RuntimeException(errorMessage));

            // when & then
            assertThatThrownBy(() -> portfolioFacade.createPortfolio(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(errorMessage);
        }
    }
}
