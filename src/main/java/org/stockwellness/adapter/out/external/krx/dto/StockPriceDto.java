package org.stockwellness.adapter.out.external.krx.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockPriceDto(
        @JsonProperty("basDt")
        String baseDate,            // 기준일자 (YYYYMMDD)

        @JsonProperty("srtnCd")
        String ticker,              // 단축코드 (예: 005930)

        @JsonProperty("isinCd")
        String isinCode,            // ISIN 코드 (예: KR7000020008)

        @JsonProperty("mrktCtg")
        String marketCategory,      // 시장구분 (KOSPI/KOSDAQ 등)

        @JsonProperty("itmsNm")
        String itemName,            // 종목명 (예: 삼성전자)

        @JsonProperty("crno")       // 법인등록번호
        String corporationNo,

        @JsonProperty("corpNm")     // 법인명
        String corporationName,

        @JsonProperty("lstgStCnt")
        String listedShares,        // 상장주식수 (중요: 시가총액 계산 검증용)

        // --- 시세 정보 (Price Data) ---
        @JsonProperty("clpr")
        String closePrice,          // 종가

        @JsonProperty("mkp")
        String openPrice,           // 시가

        @JsonProperty("hipr")
        String highPrice,           // 고가

        @JsonProperty("lopr")
        String lowPrice,            // 저가

        @JsonProperty("vs")
        String priceChange,         // 대비 (전일비)

        @JsonProperty("fltRt")
        String fluctuationRate,     // 등락률 (퍼센트)

        @JsonProperty("trqu")
        String tradingVolume,       // 거래량

        @JsonProperty("trPrc")
        String tradingValue,        // 거래대금

        @JsonProperty("mrktTotAmt")
        String marketCap            // 시가총액
) {

}
