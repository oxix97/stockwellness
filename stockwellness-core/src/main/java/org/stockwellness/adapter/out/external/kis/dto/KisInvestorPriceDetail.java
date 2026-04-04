package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 일자별 투자자 매매 추이 상세 (output)
 * TR_ID: FHKST01010900
 */
public record KisInvestorPriceDetail(
        /** 주식 영업 일자 */
        @JsonProperty("stck_bsop_date")
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate baseDate,

        /** 현재가 */
        @JsonProperty("stck_prpr")
        BigDecimal closePrice,

        /** 전일 대비 부호 */
        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,

        /** 전일 대비 */
        @JsonProperty("prdy_vrss")
        BigDecimal prdyVrss,

        /** 전일 대비율 */
        @JsonProperty("prdy_ctrt")
        BigDecimal prdyCtrt,

        /** 누적 거래량 */
        @JsonProperty("acml_vol")
        Long volume,

        /** 기관 순매수 수량 */
        @JsonProperty("ntby_inst_qty")
        Long netInstitutionalBuyingQty,

        /** 외국인 순매수 수량 */
        @JsonProperty("ntby_frgn_qty")
        Long netForeignBuyingQty,

        /** 기관 순매수 금액 */
        @JsonProperty("ntby_inst_amt")
        BigDecimal netInstitutionalBuyingAmt,

        /** 외국인 순매수 금액 */
        @JsonProperty("ntby_frgn_amt")
        BigDecimal netForeignBuyingAmt
) {}
