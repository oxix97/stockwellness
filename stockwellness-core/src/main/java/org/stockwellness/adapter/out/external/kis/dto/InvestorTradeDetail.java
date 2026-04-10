package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국내기관, 외국인 매매종목가 집계 응답
 */
public record InvestorTradeDetail(
        @JsonProperty("hts_kor_isnm")
        String htsKorIsnm,           // HTS 한글 종목명

        @JsonProperty("mksc_shrn_iscd")
        String mkscShrnIscd,         // 유가증권 단축 종목코드

        @JsonProperty("ntby_qty")
        String ntbyQty,              // 순매수 수량

        @JsonProperty("stck_prpr")
        String stckPrpr,             // 주식 현재가

        @JsonProperty("prdy_vrss_sign")
        String prdyVrssSign,         // 전일 대비 부호

        @JsonProperty("prdy_vrss")
        String prdyVrss,             // 전일 대비

        @JsonProperty("prdy_ctrt")
        String prdyCtrt,             // 전일 대비율

        @JsonProperty("acml_vol")
        String acmlVol,              // 누적 거래량

        @JsonProperty("frgn_ntby_qty")
        String frgnNtbyQty,          // 외국인 순매수 수량

        @JsonProperty("orgn_ntby_qty")
        String orgnNtbyQty,          // 기관계 순매수 수량

        @JsonProperty("ivtr_ntby_qty")
        String ivtrNtbyQty,          // 투자신탁 순매수 수량

        @JsonProperty("bank_ntby_qty")
        String bankNtbyQty,          // 은행 순매수 수량

        @JsonProperty("insu_ntby_qty")
        String insuNtbyQty,          // 보험 순매수 수량

        @JsonProperty("mrbn_ntby_qty")
        String mrbnNtbyQty,          // 종금 순매수 수량

        @JsonProperty("fund_ntby_qty")
        String fundNtbyQty,          // 기금 순매수 수량

        @JsonProperty("etc_orgt_ntby_vol")
        String etcOrgtNtbyVol,       // 기타 단체 순매수 거래량

        @JsonProperty("etc_corp_ntby_vol")
        String etcCorpNtbyVol,       // 기타 법인 순매수 거래량

        @JsonProperty("frgn_ntby_tr_pbmn")
        String frgnNtbyTrPbmn,       // 외국인 순매수 거래 대금 (단위: 백만원, 수량*현재가)

        @JsonProperty("orgn_ntby_tr_pbmn")
        String orgnNtbyTrPbmn,       // 기관계 순매수 거래 대금

        @JsonProperty("ivtr_ntby_tr_pbmn")
        String ivtrNtbyTrPbmn,       // 투자신탁 순매수 거래 대금

        @JsonProperty("bank_ntby_tr_pbmn")
        String bankNtbyTrPbmn,       // 은행 순매수 거래 대금

        @JsonProperty("insu_ntby_tr_pbmn")
        String insuNtbyTrPbmn,       // 보험 순매수 거래 대금

        @JsonProperty("mrbn_ntby_tr_pbmn")
        String mrbnNtbyTrPbmn,       // 종금 순매수 거래 대금

        @JsonProperty("fund_ntby_tr_pbmn")
        String fundNtbyTrPbmn,       // 기금 순매수 거래 대금

        @JsonProperty("etc_orgt_ntby_tr_pbmn")
        String etcOrgtNtbyTrPbmn,    // 기타 단체 순매수 거래 대금

        @JsonProperty("etc_corp_ntby_tr_pbmn")
        String etcCorpNtbyTrPbmn     // 기타 법인 순매수 거래 대금
    ) {}