package org.stockwellness.domain.portfolio.advisor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 어드바이저 조언 액션 타입
 */
@Getter
@RequiredArgsConstructor
public enum AdviceAction {
    REBALANCE("리밸런싱"),
    RISK_MANAGEMENT("리스크 관리"),
    TECHNICAL_OPTIMIZATION("기술적 최적화"),
    DIVERSIFICATION("포트폴리오 다각화");

    private final String description;
}
