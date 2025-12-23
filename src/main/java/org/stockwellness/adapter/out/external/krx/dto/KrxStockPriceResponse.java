package org.stockwellness.adapter.out.external.krx.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * API: 금융위원회_주식시세정보 (GetStockSecuritiesInfoService)
 * 기능: 주식시세 (getStockPriceInfo)
 * 문서 참조: 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KrxStockPriceResponse(
    Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
        Header header,
        Body body
    ) {}

    public record Header(
        String resultCode,
        String resultMsg
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
        int numOfRows,
        int pageNo,
        int totalCount,
        Items items
    ) {}

    public record Items(
        @JsonProperty("item") List<Item> itemList
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        String basDt,       // 기준일자
        String srtnCd,      // 단축코드 (Ticker)
        String isinCd,      // ISIN 코드
        String itmsNm,      // 종목명
        String mrktCtg,     // 시장구분 (KOSPI/KOSDAQ)
        String clpr,        // 종가 (Close)
        String mkp,         // 시가 (Open)
        String hipr,        // 고가 (High)
        String lopr,        // 저가 (Low)
        String vs,          // 대비 (전일비)
        String fltRt,       // 등락률
        String trqu,        // 거래량 (Volume)
        String mrktTotAmt,  // 시가총액
        String lstgStCnt    // 상장주식수
    ) {
    }
}