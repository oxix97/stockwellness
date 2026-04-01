package org.stockwellness.domain.stock;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BenchmarkType {
    KOSPI("0001", "코스피", false),
    KOSDAQ("1001", "코스닥", false),
    KOSPI_200("2001", "코스피 200", false),
    S_AND_P_500("SPX", "S&P 500", true),
    NASDAQ("IXIC", "나스닥", true),
    DOW_JONES("DJI", "다우 존스", true);

    private final String ticker;
    private final String description;
    private final boolean overseas;

    BenchmarkType(String ticker, String description, boolean overseas) {
        this.ticker = ticker;
        this.description = description;
        this.overseas = overseas;
    }

    public String getApiTicker() {
        if (overseas) {
            return "." + ticker; // 해외 지수의 경우 KIS API는 .SPX, .IXIC 형식을 사용함
        }
        return ticker;
    }

    public static BenchmarkType fromTicker(String ticker) {
        return Arrays.stream(values())
                .filter(b -> b.getTicker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElse(KOSPI); // 기본값
    }
}
