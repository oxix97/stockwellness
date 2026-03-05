package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisStockInfo(
        /** 전일 대비 */
        @JsonProperty("prdy_vrss") String prdyVrss,
        /** 전일 대비 부호 */
        @JsonProperty("prdy_vrss_sign") String prdyVrssSign,
        /** 전일 대비율 */
        @JsonProperty("prdy_ctrt") String prdyCtrt,
        /** 주식 전일 종가 */
        @JsonProperty("stck_prdy_clpr") String stckPrdyClpr,
        /** 누적 거래량 */
        @JsonProperty("acml_vol") String acmlVol,
        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn") String acmlTrPbmn,
        /** HTS 한글 종목명 */
        @JsonProperty("hts_kor_isnm") String htsKorIsnm,
        /** 주식 현재가 */
        @JsonProperty("stck_prpr") String stckPrpr,
        /** 주식 단축 종목코드 */
        @JsonProperty("stck_shrn_iscd") String stckShrnIscd,
        /** 전일 거래량 */
        @JsonProperty("prdy_vol") String prdyVol,
        /** 주식 상한가 */
        @JsonProperty("stck_mxpr") String stckMxpr,
        /** 주식 하한가 */
        @JsonProperty("stck_llam") String stckLlam,
        /** 주식 시가2 */
        @JsonProperty("stck_oprc") String stckOprc,
        /** 주식 최고가 */
        @JsonProperty("stck_hgpr") String stckHgpr,
        /** 주식 최저가 */
        @JsonProperty("stck_lwpr") String stckLwpr,
        /** 주식 전일 시가 */
        @JsonProperty("stck_prdy_oprc") String stckPrdyOprc,
        /** 주식 전일 최고가 */
        @JsonProperty("stck_prdy_hgpr") String stckPrdyHgpr,
        /** 주식 전일 최저가 */
        @JsonProperty("stck_prdy_lwpr") String stckPrdyLwpr,
        /** 매도호가 */
        @JsonProperty("askp") String askp,
        /** 매수호가 */
        @JsonProperty("bidp") String bidp,
        /** 전일 대비 거래량 */
        @JsonProperty("prdy_vrss_vol") String prdyVrssVol,
        /** 거래량 회전율 */
        @JsonProperty("vol_tnrt") String volTnrt,
        /** 주식 액면가 */
        @JsonProperty("stck_fcam") String stckFcam,
        /** 상장 주수 */
        @JsonProperty("lstn_stcn") String lstnStcn,
        /** 자본금 */
        @JsonProperty("cpfn") String cpfn,
        /** HTS 시가총액 */
        @JsonProperty("hts_avls") String htsAvls,
        /** PER */
        @JsonProperty("per") String per,
        /** EPS */
        @JsonProperty("eps") String eps,
        /** PBR */
        @JsonProperty("pbr") String pbr,
        /** 전체 융자 잔고 비율 */
        @JsonProperty("itewhol_loan_rmnd_ratem") String itewholLoanRmndRatem
) {
}