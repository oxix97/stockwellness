package org.stockwellness.domain.stock;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketType {
    KOSPI("코스피", BenchmarkType.KOSPI),
    KOSDAQ("코스닥", BenchmarkType.KOSDAQ),
    NASDAQ("나스닥", BenchmarkType.NASDAQ_COMPOSITE),
    NYSE("뉴욕", BenchmarkType.S_P_500),
    AMEX("아멕스", BenchmarkType.S_P_500),
    INDEX("지수", null);

    private final String description;
    private final BenchmarkType defaultBenchmark;

    public String getBenchmarkTicker() {
        return defaultBenchmark != null ? defaultBenchmark.getTicker() : "0001"; // 기본값 KOSPI
    }
}