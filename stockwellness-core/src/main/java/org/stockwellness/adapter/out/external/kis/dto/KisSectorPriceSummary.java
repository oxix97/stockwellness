package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisSectorPriceSummary(
        /** 업종 구분 코드 */
        @JsonProperty("bstp_cls_code")
        String bstpClsCode,

        /** HTS 한글 종목명 */
        @JsonProperty("hts_kor_isnm")
        String htsKorIsnm,

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

        /** 누적 거래량 비중 */
        @JsonProperty("acml_vol_rlim")
        String acmlVolRlim,

        /** 누적 거래 대금 비중 */
        @JsonProperty("acml_tr_pbmn_rlim")
        String acmlTrPbmnRlim
) {
}
