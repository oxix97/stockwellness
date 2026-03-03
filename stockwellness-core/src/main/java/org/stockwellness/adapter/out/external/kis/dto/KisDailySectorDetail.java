package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국내업종 현재지수 상세 출력 (Output)
 * TR_ID: FHPUP02100000 / FHPUP02120000
 */
public record KisDailySectorDetail(
    /** 영업 일자 (FHPUP02120000 전용) */
    @JsonProperty("stck_bsop_date") String stckBsopDate,

    /** 업종 지수 현재가 */
    @JsonProperty("bstp_nmix_prpr") String sectorIndexPrice,

    /** 업종 지수 전일 대비 */
    @JsonProperty("bstp_nmix_prdy_vrss") String sectorIndexPriceChange,

    /** 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락) */
    @JsonProperty("prdy_vrss_sign") String priceChangeSign,

    /** 업종 지수 전일 대비율 */
    @JsonProperty("bstp_nmix_prdy_ctrt") String sectorIndexPriceChangeRate,

    /** 누적 거래량 */
    @JsonProperty("acml_vol") String accumulatedVolume,

    /** 누적 거래 대금 */
    @JsonProperty("acml_tr_pbmn") String accumulatedTradingValue,

    /** 업종 지수 시가 */
    @JsonProperty("bstp_nmix_oprc") String sectorIndexOpenPrice,

    /** 업종 지수 최고가 */
    @JsonProperty("bstp_nmix_hgpr") String sectorIndexHighPrice,

    /** 업종 지수 최저가 */
    @JsonProperty("bstp_nmix_lwpr") String sectorIndexLowPrice
) {}
