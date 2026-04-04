package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.*;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.fixture.PortfolioFixture;

@ExtendWith(MockitoExtension.class)
class PortfolioDiagnosisServiceTest {

    @InjectMocks
    private PortfolioDiagnosisService portfolioDiagnosisService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private PortfolioDiagnosisDataLoader dataLoader;

    @Mock
    private PortfolioHealthCalculator healthCalculator;

    @Mock
    private LoadPortfolioAiPort loadPortfolioAiPort;

    @Mock
    private PortfolioCorrelationCalculator correlationCalculator;

    @Test
    @DisplayName("성공: 포트폴리오 진단 프로세스가 정상적으로 오케스트레이션 된다")
    void diagnose_success() {
        // Given
        Long portfolioId = 1L;
        Long memberId = 1L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Description");
        
        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));
        
        DiagnosisContext diagnosisContext = new DiagnosisContext(portfolio, Map.of(), Map.of(), null, Map.of());
        given(dataLoader.load(portfolioId)).willReturn(diagnosisContext);
        given(correlationCalculator.calculateMatrix(any())).willReturn(Map.of());

        Map<String, Integer> categories = new HashMap<>();
        categories.put(DiagnosisCategory.STABILITY.getKey(), 80);
        categories.put(DiagnosisCategory.RETURN.getKey(), 70);
        categories.put(DiagnosisCategory.AGILITY.getKey(), 90);
        categories.put(DiagnosisCategory.DIVERSIFICATION.getKey(), 85);
        categories.put(DiagnosisCategory.CASH.getKey(), 60);
        
        CalculatedHealth calculatedHealth = new CalculatedHealth(77, categories, List.of());
        given(healthCalculator.calculate(any(DiagnosisContext.class))).willReturn(calculatedHealth);

        given(loadPortfolioAiPort.generatePortfolioInsight(any(PortfolioAiContext.class)))
                .willReturn(new PortfolioAiResult("Summary", "Insight", List.of("Step 1")));

        // When
        PortfolioHealthResult result = portfolioDiagnosisService.diagnosePortfolio(memberId, portfolioId);

        // Then
        assertThat(result.overallScore()).isEqualTo(77);
        assertThat(result.categories().get(DiagnosisCategory.STABILITY.getKey())).isEqualTo(80);
        assertThat(result.summary()).isEqualTo("Summary");
    }

    @Test
    @DisplayName("실패: 다른 사용자의 포트폴리오를 진단하려고 하면 UNAUTHORIZED 예외가 발생한다")
    void fail_unauthorized() {
        // given
        Long portfolioId = 1L;
        Long otherMemberId = 999L;
        given(portfolioPort.loadPortfolio(portfolioId, otherMemberId)).willReturn(Optional.empty());
        given(portfolioPort.findById(portfolioId)).willReturn(Optional.of(PortfolioFixture.createEntity(portfolioId)));

        // when & then
        assertThatThrownBy(() -> portfolioDiagnosisService.diagnosePortfolio(otherMemberId, portfolioId))
                .isInstanceOf(PortfolioAccessDeniedException.class);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 포트폴리오를 진단하면 PORTFOLIO_NOT_FOUND 예외가 발생한다")
    void fail_not_found() {
        // given
        Long portfolioId = 1L;
        Long memberId = 1L;
        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.empty());
        given(portfolioPort.findById(portfolioId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> portfolioDiagnosisService.diagnosePortfolio(memberId, portfolioId))
                .isInstanceOf(PortfolioNotFoundException.class);
    }
}
