package org.stockwellness.application.port.in.portfolio.result;

import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import java.time.LocalDateTime;

/**
 * AI 어드바이저 조언 결과 DTO
 */
public record AdviceResponse(
        String content,
        AdviceAction action,
        LocalDateTime createdAt
) {
}
