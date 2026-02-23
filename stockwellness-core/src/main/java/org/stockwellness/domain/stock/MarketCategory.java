package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum MarketCategory {
    KOSPI_COMPOSITE("0", "코스피/대형/중소형"), // 00001~ [cite: 1]
    KOSDAQ_COMPOSITE("1", "코스닥/섹터"), // 11001~ [cite: 12]
    KOSPI_200_DERIVATIVE("2", "코스피200 및 파생"), // 22001~ [cite: 22]
    KOSDAQ_150_DERIVATIVE("3", "코스닥150 및 파생"), // 33003~ [cite: 47]
    KRX_INTEGRATED("4", "KRX 통합 지수 및 테마"), // 44001~ [cite: 53]
    FUTURES_AND_COMMODITY("6", "선물/원자재/스마트베타"), // 66001~ [cite: 71]
    EQUAL_WEIGHT("7", "동일가중지수"), // 77001~ [cite: 101]
    BOND("9", "국채/통안채"), // 99001~ [cite: 102]
    ETN("E", "상장지수증권"), // EE199 [cite: 106]
    GOLD("G", "금 현물"), // GG000 [cite: 106]
    PFC("P", "사모펀드/기타"), // PP000 [cite: 107]
    ETF("T", "상장지수펀드"), // TT000 [cite: 107]
    ELW("W", "주식워런트증권"), // WW000 [cite: 108]
    UNKNOWN("U", "미분류");

    private final String prefix;
    private final String description;

    private static final Map<String, MarketCategory> PREFIX_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(MarketCategory::getPrefix, Function.identity()));

    /**
     * 원본 코드(예: "00001", "EE199")를 받아 카테고리를 반환하는 정적 팩토리 메서드
     */
    public static MarketCategory fromCode(String fullCode) {
        if (fullCode == null || fullCode.isEmpty()) {
            return UNKNOWN;
        }

        String firstChar = fullCode.substring(0, 1).toUpperCase();
        return PREFIX_MAP.getOrDefault(firstChar, UNKNOWN);
    }
}