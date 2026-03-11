package org.stockwellness.domain.portfolio;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BacktestStrategy {
    LUMP_SUM("거치식 투자"),
    DCA("적립식 투자");

    private final String description;
}
