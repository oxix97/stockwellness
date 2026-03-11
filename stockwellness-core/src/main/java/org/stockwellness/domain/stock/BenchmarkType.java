package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum BenchmarkType {
    KOSPI("KOSPI", "코스피"),
    KOSDAQ("KOSDAQ", "코스닥"),
    S_AND_P_500("SPX", "S&P 500"),
    NASDAQ("IXIC", "나스닥");

    private final String ticker;
    private final String description;

    public static BenchmarkType fromTicker(String ticker) {
        return Arrays.stream(values())
                .filter(b -> b.getTicker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElse(KOSPI); // 기본값
    }
}
