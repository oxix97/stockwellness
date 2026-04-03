package org.stockwellness.domain.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("대기"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String description;
}
