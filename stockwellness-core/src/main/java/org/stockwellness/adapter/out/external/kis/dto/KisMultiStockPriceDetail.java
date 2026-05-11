package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 관심종목(멀티종목) 시세조회 응답 상세
 * TR_ID: FHKST11300006
 */
public record KisMultiStockPriceDetail(
        /** 관심 단축 종목코드 */
        @JsonProperty("inter_shrn_iscd") String ticker,

        /** 관심 한글 종목명 */
        @JsonProperty("inter_kor_isnm") String name,

        /** 현재가 */
        @JsonProperty("inter2_prpr") BigDecimal closePrice,

        /** 전일 대비 */
        @JsonProperty("inter2_prdy_vrss") BigDecimal priceChange,

        /** 전일 대비율 */
        @JsonProperty("prdy_ctrt") BigDecimal priceChangeRate,

        /** 시가 */
        @JsonProperty("inter2_oprc") BigDecimal openPrice,

        /** 고가 */
        @JsonProperty("inter2_hgpr") BigDecimal highPrice,

        /** 저가 */
        @JsonProperty("inter2_lwpr") BigDecimal lowPrice,

        /** 누적 거래량 */
        @JsonProperty("acml_vol") Long accumulatedVolume,

        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn") BigDecimal accumulatedTradingValue,

        /** 전일 종가 */
        @JsonProperty("inter2_prdy_clpr") BigDecimal previousClosePrice,

        /** 기관 순매수 금액 */
        @JsonProperty("ntby_amt") BigDecimal netInstitutionalBuyingAmt,

        /** 외국인 순매수 금액 */
        @JsonProperty("frgn_ntby_amt") BigDecimal netForeignBuyingAmt
) {
}
