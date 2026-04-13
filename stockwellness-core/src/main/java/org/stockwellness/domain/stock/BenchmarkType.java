package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum BenchmarkType {
    // 국내 지수 (KIS 국내 마스터 코드 기준)
    KOSPI("0001", "코스피 종합", "코스피 종합지수", false),
    KOSDAQ("1001", "코스닥 종합", "코스닥 종합지수", false),
    KOSPI_200("2001", "코스피 200", "코스피 200 지수", false),
    KOSDAQ_150("3003", "코스닥 150", "코스닥 150 지수", false),

    // 해외 지수 (frgn_code.mst 마스터 코드 기준)
    S_P_500("SPX", "S&P 500", "미국 대형주 500개 종목 산출 지수", true),
    NASDAQ_COMPOSITE("COMP", "나스닥 종합", "나스닥 시장 상장 모든 기업 대상 지수", true),
    NASDAQ_100("NDX", "나스닥 100", "나스닥 상위 100개 우량 기술주 지수", true),
    PHLX_SEMICONDUCTOR("SOX", "필라델피아 반도체", "반도체 설계/제조 관련 대표 지수", true),
    DOW_JONES(".DJI", "다우존스 산업", "미국 대표 우량주 30개 종목 산출 지수", true);

    private final String ticker; // 마스터 파일(.mst)에 정의된 코드
    private final String name;
    private final String description;
    private final boolean isOverseas;

    public static List<BenchmarkType> defaultSimulationBenchmarks() {
        return List.of(KOSPI_200, S_P_500, NASDAQ_100, DOW_JONES);
    }

    public static List<String> defaultSimulationBenchmarkTickers() {
        return defaultSimulationBenchmarks().stream()
                .map(BenchmarkType::getTicker)
                .toList();
    }

    public static BenchmarkType fromTicker(String ticker) {
        return Arrays.stream(values())
                .filter(b -> b.getTicker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElse(KOSPI);
    }
}
