package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.DiagnosePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.LoadPortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.ManagePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.PortfolioAnalysisUseCase;
import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAnalysisSummaryResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Portfolio Facade
 * 포트폴리오 관련 모든 도메인 서비스(생성, 조회, 분석, 진단)를 통합하는 단일 진입점입니다.
 * 컨트롤러와 서비스 간의 복잡한 의존성을 관리하고 서비스 간 조율 로직을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class PortfolioFacade {

    private final ManagePortfolioUseCase managePortfolioUseCase;
    private final LoadPortfolioUseCase loadPortfolioUseCase;
    private final PortfolioAnalysisUseCase portfolioAnalysisUseCase;
    private final DiagnosePortfolioUseCase diagnosePortfolioUseCase;

    // -- 포트폴리오 관리 (Manage) --

    public Long createPortfolio(CreatePortfolioCommand command) {
        return managePortfolioUseCase.createPortfolio(command);
    }

    public void updatePortfolio(UpdatePortfolioCommand command) {
        managePortfolioUseCase.updatePortfolio(command);
    }

    public void deletePortfolio(Long memberId, Long portfolioId) {
        managePortfolioUseCase.deletePortfolio(memberId, portfolioId);
    }

    // -- 포트폴리오 조회 (Load) --

    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        return loadPortfolioUseCase.getMyPortfolios(memberId);
    }

    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        return loadPortfolioUseCase.getPortfolio(memberId, portfolioId);
    }

    // -- 포트폴리오 분석 (Analysis) --

    public PortfolioValuationResult getValuation(Long memberId, Long portfolioId) {
        return portfolioAnalysisUseCase.getValuation(memberId, portfolioId);
    }

    public PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId) {
        return portfolioAnalysisUseCase.getDiversification(memberId, portfolioId);
    }

    public PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId) {
        return portfolioAnalysisUseCase.getRebalancingGuide(memberId, portfolioId);
    }

    public PortfolioAnalysisSummaryResult getAnalysisSummary(Long memberId, Long portfolioId) {
        return portfolioAnalysisUseCase.getAnalysisSummary(memberId, portfolioId);
    }

    public BacktestResult runBacktest(BacktestPortfolioCommand command) {
        return portfolioAnalysisUseCase.runBacktest(command);
    }

    public Map<String, Map<String, BigDecimal>> getCorrelationMatrix(Long memberId, Long portfolioId) {
        return portfolioAnalysisUseCase.getCorrelationMatrix(memberId, portfolioId);
    }

    // -- 포트폴리오 진단 (Diagnosis) --

    public PortfolioHealthResult diagnosePortfolio(Long memberId, Long portfolioId) {
        return diagnosePortfolioUseCase.diagnosePortfolio(memberId, portfolioId);
    }
}
