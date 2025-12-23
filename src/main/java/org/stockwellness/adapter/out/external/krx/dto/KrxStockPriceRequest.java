package org.stockwellness.adapter.out.external.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 금융위원회_주식시세정보 (GetStockSecuritiesInfoService) 요청 DTO
 * API Operation: getStockPriceInfo
 * 관련 문서: , 
 */
public record KrxStockPriceRequest(

    @JsonProperty("numOfRows")
    Integer numOfRows,          // 한 페이지 결과 수 

    @JsonProperty("pageNo")
    Integer pageNo,             // 페이지 번호 

    @JsonProperty("resultType")
    String resultType,          // json 고정 사용 

    @JsonProperty("basDt")
    String baseDate,            // 기준일자 (정확히 일치하는 날짜 조회 시) 

    @JsonProperty("beginBasDt")
    String beginBaseDate,       // 기준일자 검색 시작일 (YYYYMMDD) - 배치 최적화용 

    @JsonProperty("endBasDt")
    String endBaseDate,         // 기준일자 검색 종료일 (YYYYMMDD) - 배치 최적화용 

    @JsonProperty("isinCd")
    String isinCode,            // ISIN 코드 

    @JsonProperty("itmsNm")
    String itemName,            // 종목명 

    @JsonProperty("mrktCls")
    String marketClass,         // 시장구분 (KOSPI, KOSDAQ, KONEX) 

    // 가격 필터링 옵션 (AI 학습용으로 특정 변동폭 이상의 종목만 스크리닝할 때 유용 가능성 있음)
    @JsonProperty("beginFltRt")
    Double beginFluctuationRate, // 등락률 하한선 

    @JsonProperty("endFltRt")
    Double endFluctuationRate,   // 등락률 상한선 

    @JsonProperty("beginTrqu")
    Long beginTradingVolume,    // 거래량 하한선 

    @JsonProperty("beginMrktTotAmt")
    Long beginMarketCap         // 시가총액 하한선 (대형주 위주 필터링 시 사용) 

) {
    // EOD 배치 작업을 위한 팩토리 메서드
    public static KrxStockPriceRequest forBatch(String targetDate, int pageNo) {
        return new KrxStockPriceRequest(
            2000, // 한 번에 최대한 많이 조회 (최대값 확인 필요, 통상 1000~3000)
            pageNo,
            "json",
            targetDate, // basDt에 값을 넣으면 해당 일자의 전 종목 시세 조회 가능
            null, null, null, null, null, null, null, null, null
        );
    }
    
    // 특정 종목의 기간별 히스토리 조회용 (초기 데이터 적재용)
    public static KrxStockPriceRequest forHistory(String isinCode, String startDate, String endDate) {
         return new KrxStockPriceRequest(
            3650, // 10년치 (예시)
            1,
            "json",
            null, 
            startDate, 
            endDate, 
            isinCode, 
            null, null, null, null, null, null
        );
    }
}