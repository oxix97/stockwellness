package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        return price(ovrsNmixOprc);
    }

    @Override
    public BigDecimal highPrice() {
        return price(ovrsNmixHgpr);
    }

    @Override
    public BigDecimal lowPrice() {
        return price(ovrsNmixLwpr);
    }

    @Override
    public BigDecimal closePrice() {
        return price(ovrsNmixPrpr);
    }

    @Override
    public BigDecimal prdyVrss() {
        // 현재는 BenchmarkPriceSyncJob 단계에서 이전 영업일 종가와 비교하여 직접 계산하여 처리 중입니다.
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal prdyCtrt() {
        // 현재는 BenchmarkPriceSyncJob 단계에서 (현재가 - 전일종가) / 전일종가 로 직접 계산하여 처리 중입니다.
        return BigDecimal.ZERO;
    }

    @Override
    public Long volume() {
        return acmlVol != null ? Long.parseLong(acmlVol) : 0L;
    }

    private BigDecimal price(String raw) {
        return raw == null || raw.isBlank() ? null : new BigDecimal(raw);
    }
}
