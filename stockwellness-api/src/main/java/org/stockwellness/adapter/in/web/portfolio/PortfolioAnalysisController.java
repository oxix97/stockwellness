package org.stockwellness.adapter.in.web.portfolio;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.portfolio.dto.*;
import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAnalysisSummaryResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.global.security.MemberPrincipal;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/analysis")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private final PortfolioFacade portfolioFacade;

    /**
     * 포트폴리오 성과 분석 (가치 및 수익률)
     */
    @GetMapping("/valuation")
    public ResponseEntity<PortfolioValuationResponse> getValuation(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioValuationResult result = portfolioFacade.getValuation(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(PortfolioValuationResponse.from(result));
    }

    /**
     * 포트폴리오 비중 분석 (자산군, 업종, 국가)
     */
    @GetMapping("/diversification")
    public ResponseEntity<PortfolioDiversificationResponse> getDiversification(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioDiversificationResult result = portfolioFacade.getDiversification(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(PortfolioDiversificationResponse.from(result));
    }

    /**
     * 포트폴리오 리밸런싱 가이드 조회
     */
    @GetMapping("/rebalancing")
    public ResponseEntity<PortfolioRebalancingResponse> getRebalancingGuide(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioRebalancingResult result = portfolioFacade.getRebalancingGuide(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(PortfolioRebalancingResponse.from(result));
    }

    /**
     * 포트폴리오 분석 요약 정보 조회
     */
    @GetMapping("/summary")
    public ResponseEntity<PortfolioAnalysisSummaryResponse> getAnalysisSummary(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioAnalysisSummaryResult result = portfolioFacade.getAnalysisSummary(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(new PortfolioAnalysisSummaryResponse(
                PortfolioValuationResponse.from(result.valuation()),
                PortfolioDiversificationResponse.from(result.diversification()),
                PortfolioRebalancingResponse.from(result.rebalancing())
        ));
    }

    /**
     * 포트폴리오 백테스팅 시뮬레이션
     */
    @PostMapping("/backtest")
    public ResponseEntity<BacktestResponse> runBacktest(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId,
            @RequestBody @Valid BacktestRequest request) {

        BacktestPortfolioCommand command = new BacktestPortfolioCommand(
                memberPrincipal.id(),
                portfolioId,
                request.strategy(),
                request.amount(),
                request.benchmarkTicker()
        );

        BacktestResult result = portfolioFacade.runBacktest(command);
        return ResponseEntity.ok(BacktestResponse.from(result));
    }

    /**
     * 포트폴리오 종목 간 상관관계 행렬 조회
     */
    @GetMapping("/correlation")
    public ResponseEntity<Map<String, Map<String, BigDecimal>>> getCorrelationMatrix(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        Map<String, Map<String, BigDecimal>> matrix = portfolioFacade.getCorrelationMatrix(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(matrix);
    }
}
