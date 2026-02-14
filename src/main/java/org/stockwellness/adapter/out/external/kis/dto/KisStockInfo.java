package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisStockInfo(
        @JsonProperty("stck_shrn_iscd") String ticker,        // 종목코드 (005930)
        @JsonProperty("hts_kor_isnm") String nameKr,          // 종목명 (삼성전자)

        @JsonProperty("stck_prpr") String currentPrice,       // 현재가
        @JsonProperty("prdy_vrss") String changeAmount,       // 전일대비 (2600)
        @JsonProperty("prdy_vrss_sign") String changeSign,    // 전일대비 부호 (2:상승)
        @JsonProperty("prdy_ctrt") String changeRate,         // 전일대비율 (1.46)

        @JsonProperty("acml_vol") String accumulatedVolume,   // 누적 거래량
        @JsonProperty("acml_tr_pbmn") String accumulatedTxAmt,// 누적 거래대금

        @JsonProperty("stck_oprc") String openPrice,          // 시가
        @JsonProperty("stck_hgpr") String highPrice,          // 고가
        @JsonProperty("stck_lwpr") String lowPrice,           // 저가
        @JsonProperty("stck_mxpr") String upperLimitPrice,    // 상한가
        @JsonProperty("stck_llam") String lowerLimitPrice,    // 하한가

        @JsonProperty("per") String per,
        @JsonProperty("pbr") String pbr,
        @JsonProperty("eps") String eps,

        @JsonProperty("lstn_stcn") String listedShares        // 상장 주식수
) {}