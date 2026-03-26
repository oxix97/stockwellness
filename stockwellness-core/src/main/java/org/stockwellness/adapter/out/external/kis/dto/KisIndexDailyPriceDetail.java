package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 지수 일별 시세 상세 (output2)
 * FID_COND_MRKT_DIV_CODE=U (국내지수) 기준
 */
public record KisIndexDailyPriceDetail(
        /** 영업 일자 (yyyyMMdd -> LocalDate 변환) */
        @JsonProperty("stck_bsop_date")
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate baseDate,

        /** 업종 지수 시가 */
        @JsonProperty("bstp_nmix_oprc")
        BigDecimal openPrice,

        /** 업종 지수 최고가 */
        @JsonProperty("bstp_nmix_hgpr")
        BigDecimal highPrice,

        /** 업종 지수 최저가 */
        @JsonProperty("bstp_nmix_lwpr")
        BigDecimal lowPrice,

        /** 업종 지수 현재가 (종가) */
        @JsonProperty("bstp_nmix_prpr")
        BigDecimal closePrice,

        /** 누적 거래량 */
        @JsonProperty("acml_vol")
        Long volume,

        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn")
        BigDecimal transactionAmt,

        /** 전일 대비 부호 */
        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,

        /** 전일 대비 */
        @JsonProperty("prdy_vrss")
        BigDecimal prdyVrss,

        /** 전일 대비 등락율 */
        @JsonProperty("prdy_ctrt")
        BigDecimal prdyCtrt
) {}
