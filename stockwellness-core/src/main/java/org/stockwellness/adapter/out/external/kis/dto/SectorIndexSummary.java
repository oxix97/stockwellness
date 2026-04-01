package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 응답상세1: 업종 지수 기본 정보
 */
public record SectorIndexSummary(
        @JsonProperty("prdy_vrss_sign") String prdyVrssSign,           // 전일 대비 부호
        @JsonProperty("bstp_nmix_prdy_ctrt") String bstpNmixPrdyCtrt,  // 업종 지수 전일 대비율
        @JsonProperty("prdy_nmix") String prdyNmix,                    // 전일 지수
        @JsonProperty("acml_vol") String acmlVol,                      // 누적 거래량
        @JsonProperty("acml_tr_pbmn") String acmlTrPbmn,               // 누적 거래 대금
        @JsonProperty("hts_kor_isnm") String htsKorIsnm,               // HTS 한글 종목명
        @JsonProperty("bstp_nmix_prpr") String bstpNmixPrpr,           // 업종 지수 현재가
        @JsonProperty("bstp_cls_code") String bstpClsCode,             // 업종 구분 코드
        @JsonProperty("prdy_vol") String prdyVol,                      // 전일 거래량
        @JsonProperty("bstp_nmix_oprc") String bstpNmixOprc,           // 업종 지수 시가2
        @JsonProperty("bstp_nmix_hgpr") String bstpNmixHgpr,           // 업종 지수 최고가
        @JsonProperty("bstp_nmix_lwpr") String bstpNmixLwpr,           // 업종 지수 최저가
        @JsonProperty("futs_prdy_oprc") String futsPrdyOprc,           // 선물 전일 시가
        @JsonProperty("futs_prdy_hgpr") String futsPrdyHgpr,           // 선물 전일 최고가
        @JsonProperty("futs_prdy_lwpr") String futsPrdyLwpr
) {}