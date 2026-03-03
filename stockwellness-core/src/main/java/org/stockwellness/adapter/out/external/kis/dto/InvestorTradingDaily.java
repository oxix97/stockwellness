package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 시장별 투자자 매매동향 (일별) 응답 상세
 */
public record InvestorTradingDaily(

        /** 주식 영업 일자 */
        @JsonProperty("stck_bsop_date")
        String stckBsopDate,

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

        /** 업종 지수 시가2 */
        @JsonProperty("bstp_nmix_oprc")
        String bstpNmixOprc,

        /** 업종 지수 최고가 */
        @JsonProperty("bstp_nmix_hgpr")
        String bstpNmixHgpr,

        /** 업종 지수 최저가 */
        @JsonProperty("bstp_nmix_lwpr")
        String bstpNmixLwpr,

        /** 주식 전일 종가 */
        @JsonProperty("stck_prdy_clpr")
        String stckPrdyClpr,

        // ==================== 순매수 수량 ====================

        /** 외국인 순매수 수량 */
        @JsonProperty("frgn_ntby_qty")
        String frgnNtbyQty,

        /** 외국인 등록 순매수 수량 */
        @JsonProperty("frgn_reg_ntby_qty")
        String frgnRegNtbyQty,

        /** 외국인 비등록 순매수 수량 */
        @JsonProperty("frgn_nreg_ntby_qty")
        String frgnNregNtbyQty,

        /** 개인 순매수 수량 */
        @JsonProperty("prsn_ntby_qty")
        String prsnNtbyQty,

        /** 기관계 순매수 수량 */
        @JsonProperty("orgn_ntby_qty")
        String orgnNtbyQty,

        /** 증권 순매수 수량 */
        @JsonProperty("scrt_ntby_qty")
        String scrtNtbyQty,

        /** 투자신탁 순매수 수량 */
        @JsonProperty("ivtr_ntby_qty")
        String ivtrNtbyQty,

        /** 사모 펀드 순매수 거래량 */
        @JsonProperty("pe_fund_ntby_vol")
        String peFundNtbyVol,

        /** 은행 순매수 수량 */
        @JsonProperty("bank_ntby_qty")
        String bankNtbyQty,

        /** 보험 순매수 수량 */
        @JsonProperty("insu_ntby_qty")
        String insuNtbyQty,

        /** 종금 순매수 수량 */
        @JsonProperty("mrbn_ntby_qty")
        String mrbnNtbyQty,

        /** 기금 순매수 수량 */
        @JsonProperty("fund_ntby_qty")
        String fundNtbyQty,

        /** 기타 순매수 수량 */
        @JsonProperty("etc_ntby_qty")
        String etcNtbyQty,

        /** 기타 단체 순매수 거래량 */
        @JsonProperty("etc_orgt_ntby_vol")
        String etcOrgtNtbyVol,

        /** 기타 법인 순매수 거래량 */
        @JsonProperty("etc_corp_ntby_vol")
        String etcCorpNtbyVol,

        // ==================== 순매수 거래 대금 ====================

        /** 외국인 순매수 거래 대금 */
        @JsonProperty("frgn_ntby_tr_pbmn")
        String frgnNtbyTrPbmn,

        /** 외국인 등록 순매수 대금 */
        @JsonProperty("frgn_reg_ntby_pbmn")
        String frgnRegNtbyPbmn,

        /** 외국인 비등록 순매수 대금 */
        @JsonProperty("frgn_nreg_ntby_pbmn")
        String frgnNregNtbyPbmn,

        /** 개인 순매수 거래 대금 */
        @JsonProperty("prsn_ntby_tr_pbmn")
        String prsnNtbyTrPbmn,

        /** 기관계 순매수 거래 대금 */
        @JsonProperty("orgn_ntby_tr_pbmn")
        String orgnNtbyTrPbmn,

        /** 증권 순매수 거래 대금 */
        @JsonProperty("scrt_ntby_tr_pbmn")
        String scrtNtbyTrPbmn,

        /** 투자신탁 순매수 거래 대금 */
        @JsonProperty("ivtr_ntby_tr_pbmn")
        String ivtrNtbyTrPbmn,

        /** 사모 펀드 순매수 거래 대금 */
        @JsonProperty("pe_fund_ntby_tr_pbmn")
        String peFundNtbyTrPbmn,

        /** 은행 순매수 거래 대금 */
        @JsonProperty("bank_ntby_tr_pbmn")
        String bankNtbyTrPbmn,

        /** 보험 순매수 거래 대금 */
        @JsonProperty("insu_ntby_tr_pbmn")
        String insuNtbyTrPbmn,

        /** 종금 순매수 거래 대금 */
        @JsonProperty("mrbn_ntby_tr_pbmn")
        String mrbnNtbyTrPbmn,

        /** 기금 순매수 거래 대금 */
        @JsonProperty("fund_ntby_tr_pbmn")
        String fundNtbyTrPbmn,

        /** 기타 순매수 거래 대금 */
        @JsonProperty("etc_ntby_tr_pbmn")
        String etcNtbyTrPbmn,

        /** 기타 단체 순매수 거래 대금 */
        @JsonProperty("etc_orgt_ntby_tr_pbmn")
        String etcOrgtNtbyTrPbmn,

        /** 기타 법인 순매수 거래 대금 */
        @JsonProperty("etc_corp_ntby_tr_pbmn")
        String etcCorpNtbyTrPbmn
) {}