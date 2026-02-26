package org.stockwellness.adapter.out.external.kis.dto;

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
        @JsonProperty("inter2_prpr") String closePrice,

        /** 전일 대비 */
        @JsonProperty("inter2_prdy_vrss") String priceChange,

        /** 전일 대비율 */
        @JsonProperty("prdy_ctrt") String priceChangeRate,

        /** 시가 */
        @JsonProperty("inter2_oprc") String openPrice,

        /** 고가 */
        @JsonProperty("inter2_hgpr") String highPrice,

        /** 저가 */
        @JsonProperty("inter2_lwpr") String lowPrice,

        /** 누적 거래량 */
        @JsonProperty("acml_vol") String accumulatedVolume,

        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn") String accumulatedTradingValue,

        /** 전일 종가 */
        @JsonProperty("inter2_prdy_clpr") String previousClosePrice
) {}
