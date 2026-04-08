package org.stockwellness.adapter.in.web.portfolio;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.portfolio.dto.*;
import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAnalysisSummaryResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioInceptionPerformanceResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.security.MemberPrincipal;

import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 포트폴리오 분석 API 컨트롤러
 * 평가 가치, 자산 분산도, 리밸런싱 가이드 제공 및 백테스트 시뮬레이션 등의 분석 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/analysis")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private final PortfolioFacade portfolioFacade;

    /**
     * 포트폴리오의 실시간 평가 가치 및 손익 정보를 조회합니다.
     * 매수 금액 대비 현재 평가액, 총 수익률, 일별 손익 등을 반환합니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @return 평가 가치 정보 응답 DTO
     */
    @GetMapping("/valuation")
    public ApiResponse<PortfolioValuationResponse> getValuation(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioValuationResult result = portfolioFacade.getValuation(memberPrincipal.id(), portfolioId);
        return ApiResponse.success(PortfolioValuationResponse.from(result));
    }

    /**
     * 포트폴리오의 비중 분산 상태를 분석합니다.
     * 자산군(주식/현금), 업종별, 국가별 투자 비중(%) 정보를 제공합니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @return 분산 분석 정보 응답 DTO
     */
    @GetMapping("/diversification")
    public ApiResponse<PortfolioDiversificationResponse> getDiversification(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioDiversificationResult result = portfolioFacade.getDiversification(memberPrincipal.id(), portfolioId);
        return ApiResponse.success(PortfolioDiversificationResponse.from(result));
    }

    /**
     * 설정된 목표 비중과 현재 실시간 비중을 비교하여 리밸런싱 가이드를 조회합니다.
     * 목표 비중에 도달하기 위해 각 종목별로 추가 매수 또는 매도해야 할 추천 수량을 제공합니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @return 리밸런싱 가이드 정보 응답 DTO
     */
    @GetMapping("/rebalancing")
    public ApiResponse<PortfolioRebalancingResponse> getRebalancingGuide(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioRebalancingResult result = portfolioFacade.getRebalancingGuide(memberPrincipal.id(), portfolioId);
        return ApiResponse.success(PortfolioRebalancingResponse.from(result));
    }

    /**
     * 포트폴리오 분석의 핵심 지표(가치, 분산, 리밸런싱, 종목 기여도)를 통합하여 요약 조회합니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @param startDate 분석 시작일 (선택)
     * @param endDate 분석 종료일 (선택)
     * @return 통합 분석 요약 정보 응답 DTO
     */
    @GetMapping("/summary")
    public ApiResponse<PortfolioAnalysisSummaryResponse> getAnalysisSummary(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        PortfolioAnalysisSummaryResult result = portfolioFacade.getAnalysisSummary(memberPrincipal.id(), portfolioId, startDate, endDate);
        return ApiResponse.success(new PortfolioAnalysisSummaryResponse(
                PortfolioValuationResponse.from(result.valuation()),
                PortfolioDiversificationResponse.from(result.diversification()),
                PortfolioRebalancingResponse.from(result.rebalancing()),
                result.itemContributions()
        ));
    }

    /**
     * 특정 투자 전략(거치식/적립식)과 금액을 바탕으로 과거 수익률 백테스트 시뮬레이션을 실행합니다.
     * 결과에는 일별 수익률 곡선, CAGR, MDD, 샤프 지수 및 AI 어드바이저의 분석 코멘트가 포함됩니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @param request 백테스트 설정 요청 데이터 (전략, 금액, 리밸런싱 주기 등)
     * @return 백테스트 결과 응답 DTO
     */
    @PostMapping("/backtest")
    public ApiResponse<BacktestResponse> runBacktest(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId,
            @RequestBody @Valid BacktestRequest request) {

        BacktestPortfolioCommand command = new BacktestPortfolioCommand(
                memberPrincipal.id(),
                portfolioId,
                request.strategy(),
                request.amount(),
                java.util.List.of(
                        (request.benchmarkTicker() == null || request.benchmarkTicker().isBlank())
                                ? org.stockwellness.domain.stock.BenchmarkType.KOSPI.getTicker()
                                : request.benchmarkTicker()
                ),
                request.rebalancingPeriod(),
                request.weights()
        );

        BacktestResult result = portfolioFacade.runBacktest(command);
        String primaryTicker = command.benchmarkTickers().get(0);
        return ApiResponse.success(BacktestResponse.from(result, primaryTicker));
    }

    /**
     * 포트폴리오 구성 종목 간의 가격 변동 상관관계 행렬을 조회합니다.
     * 종목들이 얼마나 비슷하게 또는 다르게 움직이는지 수치화하여 분산 효과를 확인합니다.
     *
     * @param memberPrincipal 인증된 사용자 정보
     * @param portfolioId 포트폴리오 ID
     * @return 종목 간 상관관계 수치 (Map 형태)
     */
    @GetMapping("/correlation")
    public ApiResponse<Map<String, Map<String, BigDecimal>>> getCorrelationMatrix(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        Map<String, Map<String, BigDecimal>> matrix = portfolioFacade.getCorrelationMatrix(memberPrincipal.id(), portfolioId);
        return ApiResponse.success(matrix);
    }

    /**
     * 포트폴리오 생성 시점 기준 상세 성과 분석 조회
     */
    @GetMapping("/performance/inception")
    public ApiResponse<PortfolioInceptionPerformanceResponse> getPerformanceSinceInception(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioInceptionPerformanceResult result = portfolioFacade.getPerformanceSinceInception(memberPrincipal.id(), portfolioId);
        return ApiResponse.success(PortfolioInceptionPerformanceResponse.from(result));
    }
}
