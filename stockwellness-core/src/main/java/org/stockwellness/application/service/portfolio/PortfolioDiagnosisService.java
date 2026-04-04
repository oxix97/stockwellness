package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.portfolio.DiagnosePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.*;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.logging.LogExecution;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 진단 서비스
 * 포트폴리오의 건강 상태를 다각도로 분석하고, AI 인사이트를 포함한 종합 진단 결과를 생성합니다.
 */
@Service
@LogExecution
@RequiredArgsConstructor
public class PortfolioDiagnosisService implements DiagnosePortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final PortfolioDiagnosisDataLoader dataLoader;
    private final PortfolioHealthCalculator healthCalculator;
    private final LoadPortfolioAiPort loadPortfolioAiPort;
    private final PortfolioCorrelationCalculator correlationCalculator;

    /**
     * 특정 포트폴리오에 대한 종합 진단을 수행합니다.
     * 권한 확인부터 통계적 분석, AI 기반 전략 제안까지의 전 과정을 조율합니다.
     *
     * @param memberId 사용자 ID
     * @param portfolioId 진단할 포트폴리오 ID
     * @return 종합 건강 점수, 카테고리별 점수, AI 분석 리포트 등이 포함된 진단 결과
     */
    @Override
    public PortfolioHealthResult diagnosePortfolio(Long memberId, Long portfolioId) {
        // 1. 권한 확인 (사용자가 해당 포트폴리오의 소유자인지 체크)
        portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        // 2. 진단에 필요한 데이터 로딩 (포트폴리오 구성, 종목 정보, 과거 시세 등)
        DiagnosisContext context = dataLoader.load(portfolioId);

        // 3. 종목 간 상관관계 행렬 산출 (분산 효과 분석용)
        Map<String, List<BigDecimal>> returnsMap = new HashMap<>();
        context.stockPriceMap().forEach((symbol, prices) -> {
            List<BigDecimal> returns = prices.stream()
                    .map(StockPrice::getClosePrice)
                    .toList();
            returnsMap.put(symbol, returns);
        });
        Map<String, Map<String, BigDecimal>> correlationMatrix = correlationCalculator.calculateMatrix(returnsMap);
        
        DiagnosisContext updatedContext = new DiagnosisContext(
                context.portfolio(), context.stockMap(), context.stockPriceMap(), 
                context.backtestResult(), correlationMatrix
        );

        // 4. 건강 점수 계산 (오각형 지표 산출 엔진 호출)
        CalculatedHealth health = healthCalculator.calculate(updatedContext);

        // 5. AI 인사이트 생성 (계산된 점수와 지표를 바탕으로 AI 어드바이저 호출)
        PortfolioAiContext aiContext = new PortfolioAiContext(health.overallScore(), health.categories());
        PortfolioAiResult aiResult = loadPortfolioAiPort.generatePortfolioInsight(aiContext);

        // 6. 결과 조합 및 최종 리포트 반환
        BacktestResult backtest = updatedContext.backtestResult();
        return new PortfolioHealthResult(
                health.overallScore(),
                health.categories(),
                health.stockContributions(),
                (backtest != null) ? backtest.mdd() : BigDecimal.ZERO,
                (backtest != null) ? backtest.relativeMdd() : BigDecimal.ZERO,
                (backtest != null) ? backtest.sharpeRatio() : BigDecimal.ZERO,
                (backtest != null) ? backtest.alpha() : BigDecimal.ZERO,
                aiResult.summary(),
                aiResult.insight(),
                aiResult.nextSteps()
        );
    }
}
