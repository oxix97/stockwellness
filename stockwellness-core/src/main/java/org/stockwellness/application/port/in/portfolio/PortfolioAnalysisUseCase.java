package org.stockwellness.application.port.in.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.result.*;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;

public interface PortfolioAnalysisUseCase {
    PortfolioValuationResult getValuation(Long memberId, Long portfolioId);
    PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId);
    PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId);
    BacktestResult runBacktest(BacktestPortfolioCommand command);
    Map<String, Map<String, BigDecimal>> getCorrelationMatrix(Long memberId, Long portfolioId);

    // 통합 요약 정보 조회
    PortfolioAnalysisSummaryResult getAnalysisSummary(Long memberId, Long portfolioId, LocalDate startDate, LocalDate endDate);

    // 포트폴리오 생성 시점 기준 성과 분석
    PortfolioInceptionPerformanceResult getPerformanceSinceInception(Long memberId, Long portfolioId);
    PortfolioInceptionChartResult getInceptionChart(Long memberId, Long portfolioId);
}
