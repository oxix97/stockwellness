package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SectorDailyPrice(
        @JsonProperty("stck_bsop_date") String stckBsopDate,   // 주식 영업 일자
        @JsonProperty("bstp_nmix_prpr") String bstpNmixPrpr,   // 업종 지수 현재가
        @JsonProperty("bstp_nmix_oprc") String bstpNmixOprc,   // 업종 지수 시가2
        @JsonProperty("bstp_nmix_hgpr") String bstpNmixHgpr,   // 업종 지수 최고가
        @JsonProperty("bstp_nmix_lwpr") String bstpNmixLwpr,   // 업종 지수 최저가
        @JsonProperty("acml_vol") String acmlVol,             // 누적 거래량
        @JsonProperty("acml_tr_pbmn") String acmlTrPbmn,       // 누적 거래 대금
        @JsonProperty("mod_yn") String modYn,                  // 변경 여부
        @JsonProperty("prdy_vrss_sign") String prdyVrssSign,       // 대비 부호
        @JsonProperty("bstp_nmix_prdy_vrss") String bstpNmixPrdyVrss, // 전일 대비
        @JsonProperty("bstp_nmix_prdy_ctrt") String bstpNmixPrdyCtrt, // 전일 대비율
        @JsonProperty("invt_new_psdg") String invtNewPsdg,         // 투자 신 심리도
        @JsonProperty("d20_dsrt") String d20Dsrt                  // 20일 이격도
) implements BenchmarkPriceData {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public LocalDate baseDate() {
        return stckBsopDate != null ? LocalDate.parse(stckBsopDate, DATE_FMT) : null;
    }

    public BigDecimal closePrice() {
        return bstpNmixPrpr != null ? new BigDecimal(bstpNmixPrpr) : BigDecimal.ZERO;
    }

    public BigDecimal openPrice() {
        return bstpNmixOprc != null ? new BigDecimal(bstpNmixOprc) : BigDecimal.ZERO;
    }

    public BigDecimal highPrice() {
        return bstpNmixHgpr != null ? new BigDecimal(bstpNmixHgpr) : BigDecimal.ZERO;
    }

    public BigDecimal lowPrice() {
        return bstpNmixLwpr != null ? new BigDecimal(bstpNmixLwpr) : BigDecimal.ZERO;
    }

    public BigDecimal prdyVrss() {
        return bstpNmixPrdyVrss != null ? new BigDecimal(bstpNmixPrdyVrss) : BigDecimal.ZERO;
    }

    public BigDecimal prdyCtrt() {
        return bstpNmixPrdyCtrt != null ? new BigDecimal(bstpNmixPrdyCtrt) : BigDecimal.ZERO;
    }

    public Long volume() {
        return acmlVol != null ? Long.parseLong(acmlVol) : 0L;
    }

}