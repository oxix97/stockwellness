package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisSectorPriceDetail(
        @JsonProperty("bstp_nmix_prpr")
        String bstpNmixPrpr,             // 업종 지수 현재가

        @JsonProperty("bstp_nmix_prdy_vrss")
        String bstpNmixPrdyVrss,         // 업종 지수 전일 대비

        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,             // 전일 대비 부호

        @JsonProperty("bstp_nmix_prdy_ctrt")
        String bstpNmixPrdyCtrt,         // 업종 지수 전일 대비율

        @JsonProperty("acml_vol")
        String acmlVol,                  // 누적 거래량

        @JsonProperty("acml_tr_pbmn")
        String acmlTrPbmn,               // 누적 거래 대금

        @JsonProperty("bstp_nmix_oprc")
        String bstpNmixOprc,             // 업종 지수 시가2

        @JsonProperty("bstp_nmix_hgpr")
        String bstpNmixHgpr,             // 업종 지수 최고가

        @JsonProperty("bstp_nmix_lwpr")
        String bstpNmixLwpr,             // 업종 지수 최저가

        @JsonProperty("prdy_vol")
        String prdyVol,                  // 전일 거래량

        @JsonProperty("ascn_issu_cnt")
        String ascnIssuCnt,              // 상승 종목 수

        @JsonProperty("down_issu_cnt")
        String downIssuCnt,              // 하락 종목 수

        @JsonProperty("stnr_issu_cnt")
        String stnrIssuCnt,              // 보합 종목 수

        @JsonProperty("uplm_issu_cnt")
        String uplmIssuCnt,              // 상한 종목 수

        @JsonProperty("lslm_issu_cnt")
        String lslmIssuCnt,              // 하한 종목 수

        @JsonProperty("prdy_tr_pbmn")
        String prdyTrPbmn,               // 전일 거래 대금

        @JsonProperty("dryy_bstp_nmix_hgpr_date")
        String dryyBstpNmixHgprDate,     // 연중 업종 지수 최고가 일자

        @JsonProperty("dryy_bstp_nmix_hgpr")
        String dryyBstpNmixHgpr,         // 연중 업종 지수 최고가

        @JsonProperty("dryy_bstp_nmix_lwpr")
        String dryyBstpNmixLwpr,         // 연중 업종 지수 최저가

        @JsonProperty("dryy_bstp_nmix_lwpr_date")
        String dryyBstpNmixLwprDate      // 연중 업종 지수 최저가 일자
) {
}