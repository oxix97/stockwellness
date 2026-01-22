package org.stockwellness.domain.stock.analysis;

import lombok.Getter;

@Getter
public enum TrendStatus {
    REGULAR("정배열 (상승추세)"),
    INVERSE("역배열 (하락추세)"),
    NEUTRAL("혼조세 (방향성 없음)");

    private final String description;

    TrendStatus(String description) {
        this.description = description;
    }

}