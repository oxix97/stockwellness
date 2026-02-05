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
import org.stockwellness.application.service.portfolio.internal.CalculatedHealth;
import org.stockwellness.application.service.portfolio.internal.DiagnosisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioDiagnosisDataLoader;
import org.stockwellness.application.service.portfolio.internal.PortfolioHealthCalculator;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

    @Test
    @DisplayName("성공: 포트폴리오 진단 프로세스가 정상적으로 오케스트레이션 된다")
    void diagnose_success() {
        // Given
        Long portfolioId = 1L;
        Long memberId = 1L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Description");
        
        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));
        
        DiagnosisContext diagnosisContext = new DiagnosisContext(portfolio, Map.of(), Map.of());
        given(dataLoader.load(portfolioId)).willReturn(diagnosisContext);

        Map<String, Integer> categories = new HashMap<>();
        categories.put(DiagnosisCategory.DEFENSE.getKey(), 80);
        categories.put(DiagnosisCategory.ATTACK.getKey(), 70);
        categories.put(DiagnosisCategory.ENDURANCE.getKey(), 90);
        categories.put(DiagnosisCategory.AGILITY.getKey(), 60);
        categories.put(DiagnosisCategory.BALANCE.getKey(), 85);
        
        CalculatedHealth calculatedHealth = new CalculatedHealth(77, categories);
        given(healthCalculator.calculate(diagnosisContext)).willReturn(calculatedHealth);

        given(loadPortfolioAiPort.generatePortfolioInsight(any(PortfolioAiContext.class)))
                .willReturn(new PortfolioAiResult("Summary", "Insight", List.of("Step 1")));

        // When
        PortfolioHealthResult result = portfolioDiagnosisService.diagnosePortfolio(memberId, portfolioId);

        // Then
        assertThat(result.overallScore()).isEqualTo(77);
        assertThat(result.categories().get(DiagnosisCategory.DEFENSE.getKey())).isEqualTo(80);
        assertThat(result.summary()).isEqualTo("Summary");
    }
}
