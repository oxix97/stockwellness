package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;

/**
 * AI 리밸런싱 조언을 얻기 위한 유스케이스
 */
public interface AdvicePortfolioUseCase {
    /**
     * 포트폴리오를 분석하여 AI 리밸런싱 조언을 생성한다.
     */
    AdviceResponse getAdvice(Long memberId, Long portfolioId);
}
