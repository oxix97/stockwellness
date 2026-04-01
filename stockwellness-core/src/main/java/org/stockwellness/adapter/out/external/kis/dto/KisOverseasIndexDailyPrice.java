package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 해외 지수 일별 시세 상세 (output2)
 */
public record KisOverseasIndexDailyPrice(
        /**
         * 영업 일자
         */
        @JsonProperty("stck_bsop_date")
        String stckBsopDate,

        /**
         * 현재가 (정수 11자리, 소수 4자리)
         */
        @JsonProperty("ovrs_nmix_prpr")
        String ovrsNmixPrpr,

        /**
         * 시가 (정수 11자리, 소수 4자리)
         */
        @JsonProperty("ovrs_nmix_oprc")
        String ovrsNmixOprc,

        /**
         * 최고가 (정수 11자리, 소수 4자리)
         */
        @JsonProperty("ovrs_nmix_hgpr")
        String ovrsNmixHgpr,

        /**
         * 최저가 (정수 11자리, 소수 4자리)
         */
        @JsonProperty("ovrs_nmix_lwpr")
        String ovrsNmixLwpr,

        /**
         * 누적 거래량
         */
        @JsonProperty("acml_vol")
        String acmlVol,

        /**
         * 변경 여부
         */
        @JsonProperty("mod_yn")
        String modYn
) implements BenchmarkPriceData {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public LocalDate baseDate() {
        return stckBsopDate != null ? LocalDate.parse(stckBsopDate, DATE_FMT) : null;
    }

    @Override
    public BigDecimal openPrice() {
        return ovrsNmixOprc != null ? new BigDecimal(ovrsNmixOprc) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal highPrice() {
        return ovrsNmixHgpr != null ? new BigDecimal(ovrsNmixHgpr) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal lowPrice() {
        return ovrsNmixLwpr != null ? new BigDecimal(ovrsNmixLwpr) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal closePrice() {
        return ovrsNmixPrpr != null ? new BigDecimal(ovrsNmixPrpr) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal prdyVrss() {
        // 해외 지수 일별 시세(FHKST03030100) output2 에는 전일 대비 필드가 없음 (필요시 전일 종가와 비교 필요)
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal prdyCtrt() {
        // 해외 지수 일별 시세(FHKST03030100) output2 에는 전일 대비율 필드가 없음
        return BigDecimal.ZERO;
    }

    @Override
    public Long volume() {
        return acmlVol != null ? Long.parseLong(acmlVol) : 0L;
    }
}
