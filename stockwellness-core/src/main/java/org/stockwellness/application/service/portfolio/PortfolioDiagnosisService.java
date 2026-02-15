package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.portfolio.DiagnosePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.CalculatedHealth;
import org.stockwellness.application.service.portfolio.internal.DiagnosisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioDiagnosisDataLoader;
import org.stockwellness.application.service.portfolio.internal.PortfolioHealthCalculator;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioDiagnosisService implements DiagnosePortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final PortfolioDiagnosisDataLoader dataLoader;
    private final PortfolioHealthCalculator healthCalculator;
    private final LoadPortfolioAiPort loadPortfolioAiPort;

    @Override
    public PortfolioHealthResult diagnosePortfolio(Long memberId, Long portfolioId) {
        // 1. 권한 확인 (소유권 체크)
        portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        // 2. 데이터 로딩 (Portfolio, Stocks, Histories)
        DiagnosisContext context = dataLoader.load(portfolioId);

        // 3. 건강 점수 계산
        CalculatedHealth health = healthCalculator.calculate(context);

        // 4. AI 인사이트 생성
        PortfolioAiContext aiContext = new PortfolioAiContext(health.overallScore(), health.categories());
        PortfolioAiResult aiResult = loadPortfolioAiPort.generatePortfolioInsight(aiContext);

        // 5. 결과 조합
        return new PortfolioHealthResult(
                health.overallScore(),
                health.categories(),
                List.of(), // TODO: Stock Contributions
                aiResult.summary(),
                aiResult.insight(),
                aiResult.nextSteps()
        );
    }
}
