package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;

/**
 * AI 어드바이저 유스케이스 (생성 및 조회 통합)
 */
public interface AiAdvisorUseCase {
    /**
     * 포트폴리오를 분석하여 새로운 AI 리밸런싱 조언을 생성한다.
     */
    AdviceResponse getNewAdvice(Long memberId, Long portfolioId);

    /**
     * 특정 포트폴리오의 가장 최근 AI 리밸런싱 조언을 조회한다.
     */
    AdviceResponse getLatestAdvice(Long memberId, Long portfolioId);
}
