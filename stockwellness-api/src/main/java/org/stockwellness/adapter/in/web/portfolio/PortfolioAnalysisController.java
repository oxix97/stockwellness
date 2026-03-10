package org.stockwellness.adapter.in.web.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioDiversificationResponse;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioValuationResponse;
import org.stockwellness.application.port.in.portfolio.PortfolioDiversificationUseCase;
import org.stockwellness.application.port.in.portfolio.PortfolioValuationUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/analysis")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private final PortfolioValuationUseCase portfolioValuationUseCase;
    private final PortfolioDiversificationUseCase portfolioDiversificationUseCase;

    /**
     * 포트폴리오 성과 분석 (가치 및 수익률)
     */
    @GetMapping("/valuation")
    public ResponseEntity<PortfolioValuationResponse> getValuation(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioValuationResult result = portfolioValuationUseCase.getValuation(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(PortfolioValuationResponse.from(result));
    }

    /**
     * 포트폴리오 비중 분석 (자산군, 업종, 국가)
     */
    @GetMapping("/diversification")
    public ResponseEntity<PortfolioDiversificationResponse> getDiversification(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioDiversificationResult result = portfolioDiversificationUseCase.getDiversification(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(PortfolioDiversificationResponse.from(result));
    }
}
