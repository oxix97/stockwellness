package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum SectorCode {
    // KOSPI/KOSDAQ 주요 업종 코드 (파트너님 리스트 반영)
    FOOD_BEVERAGE("001", "음식료품"),
    TEXTILE_APPAREL("002", "섬유의복"),
    PAPER_WOOD("003", "종이목재"),
    CHEMICAL("004", "화학"),
    PHARMACEUTICAL("005", "의약품"),
    CONSTRUCTION("017", "건설업"),
    FINANCE_ETC("194", "기타금융"),
    INDEX_ETC("000", "지수/기타"),

    // 특수 금융 상품 (문자열 코드 대응)
    ETF("ETF", "상장지수펀드"),
    ETN("ETN", "상장지수증권"),

    // 기본값
    UNKNOWN("999", "미분류");

    private final String code;
    private final String label;

    private static final Map<String, SectorCode> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(SectorCode::getCode, Function.identity()));

    /**
     * 13개 코드 조합을 해석하는 핵심 로직
     */
    public static SectorCode of(String rawCode, String name) {
        // 1. 이름 기반 우선 분류 (가장 확실함)
        if (name != null) {
            if (name.contains("ETF")) return ETF;
            if (name.contains("ETN")) return ETN;
        }

        // 2. 숫자 코드 매핑
        SectorCode sector = CODE_MAP.get(rawCode);
        if (sector != null) return sector;

        // 3. NNN, YNY 등 문자열이 들어왔는데 이름에 ETF/ETN이 없다면 UNKNOWN 처리
        // (단, 이 경우에도 데이터 정합성을 위해 UNKNOWN으로 분류하되 로그를 남길 수 있음)
        return UNKNOWN;
    }
}