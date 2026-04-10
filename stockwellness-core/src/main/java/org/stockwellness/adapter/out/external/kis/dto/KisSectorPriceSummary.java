package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisSectorPriceSummary(
        @JsonProperty("stck_bsop_date")
        String stckBsopDate,             // 주식 영업 일자

        @JsonProperty("bstp_nmix_prpr")
        String bstpNmixPrpr,             // 업종 지수 현재가

        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,             // 전일 대비 부호

        @JsonProperty("bstp_nmix_prdy_vrss")
        String bstpNmixPrdyVrss,         // 업종 지수 전일 대비

        @JsonProperty("bstp_nmix_prdy_ctrt")
        String bstpNmixPrdyCtrt,         // 업종 지수 전일 대비율

        @JsonProperty("bstp_nmix_oprc")
        String bstpNmixOprc,             // 업종 지수 시가2

        @JsonProperty("bstp_nmix_hgpr")
        String bstpNmixHgpr,             // 업종 지수 최고가

        @JsonProperty("bstp_nmix_lwpr")
        String bstpNmixLwpr,             // 업종 지수 최저가

        @JsonProperty("acml_vol_rlim")
        String acmlVolRlim,              // 누적 거래량 비중

        @JsonProperty("acml_vol")
        String acmlVol,                  // 누적 거래량

        @JsonProperty("acml_tr_pbmn")
        String acmlTrPbmn,               // 누적 거래 대금

        @JsonProperty("invt_new_psdg")
        String invtNewPsdg,              // 투자 신 심리도

        @JsonProperty("d20_dsrt")
        String d20Dsrt                   // 20일 이격도
) {
}
