package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주식 일자별 투자자 매매 추이 상세 (output)
 * TR_ID: FHKST01010900
 */
public record KisInvestorPriceDetail(
        /** 주식 영업 일자 */
        @JsonProperty("stck_bsop_date")
        String baseDate,

        /** 현재가 */
        @JsonProperty("stck_clpr")
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

        /** 개인 순매수 수량 */
        @JsonProperty("prsn_ntby_qty")
        Long netPersonBuyingQty,

        /** 외국인 순매수 수량 */
        @JsonProperty("frgn_ntby_qty")
        Long netForeignBuyingQty,

        /** 기관계 순매수 수량 */
        @JsonProperty("orgn_ntby_qty")
        Long netInstitutionalBuyingQty,

        /** 개인 순매수 거래 대금 */
        @JsonProperty("prsn_ntby_tr_pbmn")
        BigDecimal netPersonBuyingAmt,

        /** 외국인 순매수 거래 대금 */
        @JsonProperty("frgn_ntby_tr_pbmn")
        BigDecimal netForeignBuyingAmt,

        /** 기관계 순매수 거래 대금 */
        @JsonProperty("orgn_ntby_tr_pbmn")
        BigDecimal netInstitutionalBuyingAmt
) {}
