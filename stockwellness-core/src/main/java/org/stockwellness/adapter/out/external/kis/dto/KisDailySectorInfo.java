package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisDailySectorInfo(
        @JsonProperty("bstp_nmix_prpr")
        String bstpNmixPrpr,                         // 업종 지수 현재가

        @JsonProperty("bstp_nmix_prdy_vrss")
        String bstpNmixPrdyVrss,                     // 업종 지수 전일 대비

        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,                         // 전일 대비 부호

        @JsonProperty("bstp_nmix_prdy_ctrt")
        String bstpNmixPrdyCtrt,                     // 업종 지수 전일 대비율

        @JsonProperty("acml_vol")
        String acmlVol,                              // 누적 거래량

        @JsonProperty("prdy_vol")
        String prdyVol,                              // 전일 거래량

        @JsonProperty("acml_tr_pbmn")
        String acmlTrPbmn,                           // 누적 거래 대금

        @JsonProperty("prdy_tr_pbmn")
        String prdyTrPbmn,                           // 전일 거래 대금

        @JsonProperty("bstp_nmix_oprc")
        String bstpNmixOprc,                         // 업종 지수 시가

        @JsonProperty("prdy_nmix_vrss_nmix_oprc")
        String prdyNmixVrssNmixOprc,                 // 전일 지수 대비 지수 시가

        @JsonProperty("oprc_vrss_prpr_sign")
        String oprcVrssPrprSign,                     // 시가 대비 현재가 부호

        @JsonProperty("bstp_nmix_oprc_prdy_ctrt")
        String bstpNmixOprcPrdyCtrt,                 // 업종 지수 시가 전일 대비율

        @JsonProperty("bstp_nmix_hgpr")
        String bstpNmixHgpr,                         // 업종 지수 최고가

        @JsonProperty("prdy_nmix_vrss_nmix_hgpr")
        String prdyNmixVrssNmixHgpr,                 // 전일 지수 대비 지수 최고가

        @JsonProperty("hgpr_vrss_prpr_sign")
        String hgprVrssPrprSign,                     // 최고가 대비 현재가 부호

        @JsonProperty("bstp_nmix_hgpr_prdy_ctrt")
        String bstpNmixHgprPrdyCtrt,                 // 업종 지수 최고가 전일 대비율

        @JsonProperty("bstp_nmix_lwpr")
        String bstpNmixLwpr,                         // 업종 지수 최저가

        @JsonProperty("prdy_clpr_vrss_lwpr")
        String prdyClprVrssLwpr,                     // 전일 종가 대비 최저가

        @JsonProperty("lwpr_vrss_prpr_sign")
        String lwprVrssPrprSign,                     // 최저가 대비 현재가 부호

        @JsonProperty("prdy_clpr_vrss_lwpr_rate")
        String prdyClprVrssLwprRate,                 // 전일 종가 대비 최저가율

        @JsonProperty("ascn_issu_cnt")
        String ascnIssuCnt,                          // 상승 종목 수

        @JsonProperty("uplm_issu_cnt")
        String uplmIssuCnt,                          // 상한 종목 수

        @JsonProperty("stnr_issu_cnt")
        String stnrIssuCnt,                          // 보합 종목 수

        @JsonProperty("down_issu_cnt")
        String downIssuCnt,                          // 하락 종목 수

        @JsonProperty("lslm_issu_cnt")
        String lslmIssuCnt,                          // 하한 종목 수

        @JsonProperty("dryy_bstp_nmix_hgpr")
        String dryyBstpNmixHgpr,                     // 연중 업종 지수 최고가

        @JsonProperty("dryy_hgpr_vrss_prpr_rate")
        String dryyHgprVrssPrprRate,                 // 연중 최고가 대비 현재가 비율

        @JsonProperty("dryy_bstp_nmix_hgpr_date")
        String dryyBstpNmixHgprDate,                 // 연중 업종 지수 최고가 일자

        @JsonProperty("dryy_bstp_nmix_lwpr")
        String dryyBstpNmixLwpr,                     // 연중 업종 지수 최저가

        @JsonProperty("dryy_lwpr_vrss_prpr_rate")
        String dryyLwprVrssPrprRate,                 // 연중 최저가 대비 현재가 비율

        @JsonProperty("dryy_bstp_nmix_lwpr_date")
        String dryyBstpNmixLwprDate,                 // 연중 업종 지수 최저가 일자

        @JsonProperty("total_askp_rsqn")
        String totalAskpRsqn,                        // 총 매도호가 잔량

        @JsonProperty("total_bidp_rsqn")
        String totalBidpRsqn,                        // 총 매수호가 잔량

        @JsonProperty("seln_rsqn_rate")
        String selnRsqnRate,                         // 매도 잔량 비율

        @JsonProperty("shnu_rsqn_rate")
        String shnuRsqnRate,                         // 매수 잔량 비율

        @JsonProperty("ntby_rsqn")
        String ntbyRsqn
) {
}
