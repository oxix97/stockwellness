package org.stockwellness.domain.stock.price;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvestorType {
    ALL("전체(외인+기관)"),
    INSTITUTION("기관"),
    FOREIGN("외국인");

    private final String description;
}
