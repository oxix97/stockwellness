package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국내업종 현재지수 상세 출력 (Output)
 * TR_ID: FHPUP02100000
 * 
 * @see <a href="https://apiportal.koreainvestment.com">한국투자증권 오픈API 문서 [국내주식] 업종/기타 > 국내업종 현재지수</a>
 */
public record KisDailySectorDetail(
    // =================================================================================
    // 1. 지수 기본 정보
    // =================================================================================
    /** 업종 지수 현재가 */
    @JsonProperty("bstp_nmix_prpr") String sectorIndexPrice,

    /** 업종 지수 전일 대비 */
    @JsonProperty("bstp_nmix_prdy_vrss") String sectorIndexPriceChange,

    /** 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락) */
    @JsonProperty("prdy_vrss_sign") String priceChangeSign,

    /** 업종 지수 전일 대비율 */
    @JsonProperty("bstp_nmix_prdy_ctrt") String sectorIndexPriceChangeRate,

    // =================================================================================
    // 2. 거래량 및 거래대금
    // =================================================================================
    /** 누적 거래량 */
    @JsonProperty("acml_vol") String accumulatedVolume,

    /** 전일 거래량 */
    @JsonProperty("prdy_vol") String previousDayVolume,

    /** 누적 거래 대금 */
    @JsonProperty("acml_tr_pbmn") String accumulatedTradingValue,

    /** 전일 거래 대금 */
    @JsonProperty("prdy_tr_pbmn") String previousDayTradingValue,

    // =================================================================================
    // 3. 시가 / 고가 / 저가 상세 정보
    // =================================================================================
    /** 업종 지수 시가 */
    @JsonProperty("bstp_nmix_oprc") String sectorIndexOpenPrice,

    /** 전일 지수 대비 지수 시가 */
    @JsonProperty("prdy_nmix_vrss_nmix_oprc") String openPriceChange,

    /** 시가 대비 현재가 부호 */
    @JsonProperty("oprc_vrss_prpr_sign") String openPriceChangeSign,

    /** 업종 지수 시가 전일 대비율 */
    @JsonProperty("bstp_nmix_oprc_prdy_ctrt") String openPriceChangeRate,

    /** 업종 지수 최고가 */
    @JsonProperty("bstp_nmix_hgpr") String sectorIndexHighPrice,

    /** 전일 지수 대비 지수 최고가 */
    @JsonProperty("prdy_nmix_vrss_nmix_hgpr") String highPriceChange,

    /** 최고가 대비 현재가 부호 */
    @JsonProperty("hgpr_vrss_prpr_sign") String highPriceChangeSign,

    /** 업종 지수 최고가 전일 대비율 */
    @JsonProperty("bstp_nmix_hgpr_prdy_ctrt") String highPriceChangeRate,

    /** 업종 지수 최저가 */
    @JsonProperty("bstp_nmix_lwpr") String sectorIndexLowPrice,

    /** 전일 종가 대비 최저가 */
    @JsonProperty("prdy_clpr_vrss_lwpr") String lowPriceChange,

    /** 최저가 대비 현재가 부호 */
    @JsonProperty("lwpr_vrss_prpr_sign") String lowPriceChangeSign,

    /** 전일 종가 대비 최저가 비율 */
    @JsonProperty("prdy_clpr_vrss_lwpr_rate") String lowPriceChangeRate,

    // =================================================================================
    // 4. 등락 종목 수 현황
    // =================================================================================
    /** 상승 종목 수 */
    @JsonProperty("ascn_issu_cnt") String risingIssueCount,

    /** 상한 종목 수 */
    @JsonProperty("uplm_issu_cnt") String upperLimitIssueCount,

    /** 보합 종목 수 */
    @JsonProperty("stnr_issu_cnt") String steadyIssueCount,

    /** 하락 종목 수 */
    @JsonProperty("down_issu_cnt") String fallingIssueCount,

    /** 하한 종목 수 */
    @JsonProperty("lslm_issu_cnt") String lowerLimitIssueCount,

    // =================================================================================
    // 5. 연중 최고 / 최저 기록
    // =================================================================================
    /** 연중 업종 지수 최고가 */
    @JsonProperty("dryy_bstp_nmix_hgpr") String yearHighPrice,

    /** 연중 최고가 대비 현재가 비율 */
    @JsonProperty("dryy_hgpr_vrss_prpr_rate") String yearHighPriceRate,

    /** 연중 업종 지수 최고가 일자 */
    @JsonProperty("dryy_bstp_nmix_hgpr_date") String yearHighPriceDate,

    /** 연중 업종 지수 최저가 */
    @JsonProperty("dryy_bstp_nmix_lwpr") String yearLowPrice,

    /** 연중 최저가 대비 현재가 비율 */
    @JsonProperty("dryy_lwpr_vrss_prpr_rate") String yearLowPriceRate,

    /** 연중 업종 지수 최저가 일자 */
    @JsonProperty("dryy_bstp_nmix_lwpr_date") String yearLowPriceDate,

    // =================================================================================
    // 6. 호가 잔량 정보
    // =================================================================================
    /** 총 매도호가 잔량 */
    @JsonProperty("total_askp_rsqn") String totalAskResidualQuantity,

    /** 총 매수호가 잔량 */
    @JsonProperty("total_bidp_rsqn") String totalBidResidualQuantity,

    /** 매도 잔량 비율 */
    @JsonProperty("seln_rsqn_rate") String askResidualRate,

    /** 매수 잔량 비율 */
    @JsonProperty("shnu_rsqn_rate") String bidResidualRate,

    /** 순매수 잔량 */
    @JsonProperty("ntby_rsqn") String netBuyResidualQuantity
) {}