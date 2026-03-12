package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Country {
    KR("대한민국"),
    US("미국"),
    JP("일본"),
    ETC("기타");

    private final String description;
}
