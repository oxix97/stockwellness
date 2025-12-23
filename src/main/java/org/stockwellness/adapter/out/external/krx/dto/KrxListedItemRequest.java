package org.stockwellness.adapter.out.external.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 금융위원회_KRX상장종목정보 (GetKrxListedInfoService) 요청 DTO
 * API Operation: getItemInfo
 * 관련 문서: ,
 */
public record KrxListedItemRequest(
        @JsonProperty("numOfRows")
        Integer numOfRows,          // 한 페이지 결과 수 (Default: 10)

        @JsonProperty("pageNo")
        Integer pageNo,             // 페이지 번호 (Default: 1)

        @JsonProperty("resultType")
        String resultType,          // 결과형식 (xml/json) -> 우리 프로젝트는 "json" 고정

        @JsonProperty("basDt")
        String baseDate,            // 기준일자 (YYYYMMDD) - 특정 일자 기준 검색

        @JsonProperty("isinCd")
        String isinCode,            // ISIN 코드 (예: KR7005930003)

        @JsonProperty("srtnCd")
        String shortCode,           // 단축코드 (예: 005930) - 문서상 파라미터명은 likeSrtnCd 등이 혼용되나 정확한 조회를 위해 매핑

        @JsonProperty("itmsNm")
        String itemName,            // 종목명 (예: 삼성전자)

        @JsonProperty("mrktCls")
        String marketClass          // 시장구분 (KOSPI/KOSDAQ 등) - 문서에는 mrktCtg 등으로 응답되나 요청 필터링용
) {
    // 생성자 팩토리 메서드로 기본값 세팅 편의성 제공
    public static KrxListedItemRequest of(int page, String baseDate) {
        return new KrxListedItemRequest(
                1000, page, "json", baseDate, null, null, null, null
        );
    }
}