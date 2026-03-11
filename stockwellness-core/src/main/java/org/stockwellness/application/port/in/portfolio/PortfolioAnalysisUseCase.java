package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAnalysisSummaryResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;

import java.math.BigDecimal;
import java.util.Map;

public interface PortfolioAnalysisUseCase {
    PortfolioValuationResult getValuation(Long memberId, Long portfolioId);
    PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId);
    PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId);
    BacktestResult runBacktest(BacktestPortfolioCommand command);
    Map<String, Map<String, BigDecimal>> getCorrelationMatrix(Long memberId, Long portfolioId);

    // 통합 요약 정보 조회
    PortfolioAnalysisSummaryResult getAnalysisSummary(Long memberId, Long portfolioId);
}
