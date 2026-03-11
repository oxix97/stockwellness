package org.stockwellness.domain.stock.price;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceIssueType {
    NULL_PRICE("시세 데이터 누락"),
    ZERO_PRICE("0원 시세 데이터 오류"),
    GAP_EXIST("데이터 연속성 끊김(Gap)");

    private final String description;
}
