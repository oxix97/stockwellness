package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 일별 시세 상세 (output2)
 */
public record KisDailyPriceDetail(
        /** 주식 영업 일자 (yyyyMMdd -> LocalDate 변환) */
        @JsonProperty("stck_bsop_date")
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate baseDate,

        /** 주식 시가2 */
        @JsonProperty("stck_oprc")
        BigDecimal openPrice,

        /** 주식 최고가 */
        @JsonProperty("stck_hgpr")
        BigDecimal highPrice,

        /** 주식 최저가 */
        @JsonProperty("stck_lwpr")
        BigDecimal lowPrice,

        /** 주식 종가 */
        @JsonProperty("stck_clpr")
        BigDecimal closePrice,

        /** 누적 거래량 */
        @JsonProperty("acml_vol")
        Long volume,

        /** 누적 거래 대금 */
        @JsonProperty("acml_tr_pbmn")
        BigDecimal transactionAmt,

        /** * 락 구분 코드
         * 01:권리락, 02:배당락, 03:분배락, 04:권배락, 05:중간(분기)배당락, 06:권리중간배당락, 07:권리분기배당락
         */
        @JsonProperty("flng_cls_code")
        String flngClsCode,

        /** 분할 비율 (기준가/전일 종가) */
        @JsonProperty("prtt_rate")
        String prttRate,

        /** 변경 여부 (현재 영업일에 체결이 발생하지 않아 시가가 없을경우 Y 로 표시) */
        @JsonProperty("mod_yn")
        String modYn,

        /** 전일 대비 부호 */
        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,

        /** 전일 대비 */
        @JsonProperty("prdy_vrss")
        BigDecimal prdyVrss,

        /** * 재평가사유코드
         * 00:해당없음, 01:회사분할, 02:자본감소, 03:장기간정지, 04:초과분배, 05:대규모배당, 06:회사분할합병, 07:ETN증권병합/분할, 08:신종증권기세조정, 99:기타
         */
        @JsonProperty("revl_issu_reas")
        String revlIssuReas
) {}
