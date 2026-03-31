package org.stockwellness.domain.portfolio;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리밸런싱 주기 정의
 */
@Getter
@RequiredArgsConstructor
public enum RebalancingPeriod {
    NONE("없음"),
    MONTHLY("매월"),
    QUARTERLY("매분기"),
    YEARLY("매년");

    private final String description;

    public static RebalancingPeriod from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return RebalancingPeriod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
