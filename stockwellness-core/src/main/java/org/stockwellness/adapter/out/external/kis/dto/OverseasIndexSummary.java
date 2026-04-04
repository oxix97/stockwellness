package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 해외 지수 요약 정보 (output1)
 */
public record OverseasIndexSummary(
/** 전일 대비 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_prdy_vrss")
String ovrsNmixPrdyVrss,

/** 전일 대비 부호 */
@JsonProperty("prdy_vrss_sign")
String prdyVrssSign,

/** 전일 대비율 (정수 8자리, 소수 2자리) */
@JsonProperty("prdy_ctrt")
String prdyCtrt,

/** 전일 종가 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_prdy_clpr")
String ovrsNmixPrdyClpr,

/** 누적 거래량 */
@JsonProperty("acml_vol")
String acmlVol,

/** HTS 한글 종목명 */
@JsonProperty("hts_kor_isnm")
String htsKorIsnm,

/** 현재가 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_prpr")
String ovrsNmixPrpr,

/** 단축 종목코드 */
@JsonProperty("stck_shrn_iscd")
String stckShrnIscd,

/** 전일 거래량 */
@JsonProperty("prdy_vol")
String prdyVol,

/** 시가 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_oprc")
String ovrsNmixOprc,

/** 최고가 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_hgpr")
String ovrsNmixHgpr,

/** 최저가 (정수 11자리, 소수 4자리) */
@JsonProperty("ovrs_nmix_lwpr")
String ovrsNmixLwpr
) {
}
