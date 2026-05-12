package org.stockwellness.application.port.in.portfolio.result;

import java.time.LocalDateTime;

import org.stockwellness.domain.portfolio.advisor.AdviceAction;

/**
 * AI 어드바이저 조언 결과 DTO
 */
public record AdviceResponse(
        String content,
        AdviceAction action,
        LocalDateTime createdAt
) {
    public static AdviceResponse mock() {
        return new AdviceResponse(
                "Mock AI 조언입니다. 비용 절감을 위해 실제 AI 호출이 비활성화되었습니다. 현재 비중을 유지하며 시장 상황을 모니터링하세요.",
                AdviceAction.HOLD,
                LocalDateTime.now()
        );
    }
}
