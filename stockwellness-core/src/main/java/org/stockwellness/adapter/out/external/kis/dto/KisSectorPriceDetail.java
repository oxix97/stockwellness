package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisSectorPriceDetail(
        /** 업종 지수 현재가 */
        @JsonProperty("bstp_nmix_prpr")
        String bstpNmixPrpr,

        /** 업종 지수 전일 대비 */
        @JsonProperty("bstp_nmix_prdy_vrss")
        String bstpNmixPrdyVrss,

        /** 전일 대비 부호 */
        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,

        /** 업종 지수 전일 대비율 */
        @JsonProperty("bstp_nmix_prdy_ctrt")
        String bstpNmixPrdyCtrt,

        /** 누적 거래량 */
        @JsonProperty("acml_vol")
        String acmlVol,

        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn")
        String acmlTrPbmn,

        /** 업종 지수 시가2 */
        @JsonProperty("bstp_nmix_oprc")
        String bstpNmixOprc,

        /** 업종 지수 최고가 */
        @JsonProperty("bstp_nmix_hgpr")
        String bstpNmixHgpr,

        /** 업종 지수 최저가 */
        @JsonProperty("bstp_nmix_lwpr")
        String bstpNmixLwpr,

        /** 전일 거래량 */
        @JsonProperty("prdy_vol")
        String prdyVol,

        /** 상승 종목 수 */
        @JsonProperty("ascn_issu_cnt")
        String ascnIssuCnt,

        /** 하락 종목 수 */
        @JsonProperty("down_issu_cnt")
        String downIssuCnt,

        /** 보합 종목 수 */
        @JsonProperty("stnr_issu_cnt")
        String stnrIssuCnt,

        /** 상한 종목 수 */
        @JsonProperty("uplm_issu_cnt")
        String uplmIssuCnt,

        /** 하한 종목 수 */
        @JsonProperty("lslm_issu_cnt")
        String lslmIssuCnt,

        /** 전일 거래 대금 */
        @JsonProperty("prdy_tr_pbmn")
        String prdyTrPbmn,

        /** 연중 업종 지수 최고가 일자 */
        @JsonProperty("dryy_bstp_nmix_hgpr_date")
        String dryyBstpNmixHgprDate,

        /** 연중 업종 지수 최고가 */
        @JsonProperty("dryy_bstp_nmix_hgpr")
        String dryyBstpNmixHgpr,

        /** 연중 업종 지수 최저가 */
        @JsonProperty("dryy_bstp_nmix_lwpr")
        String dryyBstpNmixLwpr,

        /** 연중 업종 지수 최저가 일자 */
        @JsonProperty("dryy_bstp_nmix_lwpr_date")
        String dryyBstpNmixLwprDate
) {
}
