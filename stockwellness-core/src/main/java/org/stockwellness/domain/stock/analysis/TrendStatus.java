package org.stockwellness.domain.stock.analysis;

import lombok.Getter;

@Getter
public enum TrendStatus {
    REGULAR("정배열 (상승추세)", "#F44336"), // Red
    INVERSE("역배열 (하락추세)", "#2196F3"), // Blue
    NEUTRAL("혼조세 (방향성 없음)", "#9E9E9E"); // Grey

    private final String description;
    private final String colorCode;

    TrendStatus(String description, String colorCode) {
        this.description = description;
        this.colorCode = colorCode;
    }
}