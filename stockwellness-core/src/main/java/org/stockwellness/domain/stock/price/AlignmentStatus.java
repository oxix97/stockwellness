package org.stockwellness.domain.stock.price;

import lombok.Getter;

@Getter
public enum AlignmentStatus {
    PERFECT("정배열"), // 5 > 20 > 60 > 120
    REVERSE("역배열"), // 5 < 20 < 60 < 120
    MIXED("혼조");    // 그 외 상태

    private final String description;

    AlignmentStatus(String description) {
        this.description = description;
    }
}
