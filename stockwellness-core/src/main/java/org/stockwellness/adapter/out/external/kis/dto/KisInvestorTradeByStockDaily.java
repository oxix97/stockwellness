package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** KIS FHPTJ04160001 종목별 일별 투자자 매매 응답의 원천 값. */
public record KisInvestorTradeByStockDaily(
        @JsonProperty("stck_bsop_date") String baseDate,
        @JsonProperty("frgn_ntby_qty") String foreignNetQuantity,
        @JsonProperty("frgn_ntby_tr_pbmn") String foreignNetAmount,
        @JsonProperty("orgn_ntby_qty") String institutionNetQuantity,
        @JsonProperty("orgn_ntby_tr_pbmn") String institutionNetAmount,
        @JsonProperty("prsn_ntby_qty") String individualNetQuantity,
        @JsonProperty("prsn_ntby_tr_pbmn") String individualNetAmount,
        @JsonProperty("scrt_ntby_qty") String financialInvestmentNetQuantity,
        @JsonProperty("scrt_ntby_tr_pbmn") String financialInvestmentNetAmount,
        @JsonProperty("insu_ntby_qty") String insuranceNetQuantity,
        @JsonProperty("insu_ntby_tr_pbmn") String insuranceNetAmount,
        @JsonProperty("ivtr_ntby_qty") String trustNetQuantity,
        @JsonProperty("ivtr_ntby_tr_pbmn") String trustNetAmount,
        @JsonProperty("pe_fund_ntby_vol") String privateFundNetQuantity,
        @JsonProperty("pe_fund_ntby_tr_pbmn") String privateFundNetAmount,
        @JsonProperty("bank_ntby_qty") String bankNetQuantity,
        @JsonProperty("bank_ntby_tr_pbmn") String bankNetAmount,
        @JsonProperty("fund_ntby_qty") String pensionNetQuantity,
        @JsonProperty("fund_ntby_tr_pbmn") String pensionNetAmount,
        @JsonProperty("mrbn_ntby_qty") String merchantBankNetQuantity,
        @JsonProperty("mrbn_ntby_tr_pbmn") String merchantBankNetAmount,
        @JsonProperty("etc_orgt_ntby_vol") String otherOrganizationNetQuantity,
        @JsonProperty("etc_orgt_ntby_tr_pbmn") String otherOrganizationNetAmount,
        @JsonProperty("etc_corp_ntby_vol") String otherCorporateNetQuantity,
        @JsonProperty("etc_corp_ntby_tr_pbmn") String otherCorporateNetAmount,
        @JsonProperty("etc_ntby_qty") String otherNetQuantity,
        @JsonProperty("etc_ntby_tr_pbmn") String otherNetAmount
) {}
