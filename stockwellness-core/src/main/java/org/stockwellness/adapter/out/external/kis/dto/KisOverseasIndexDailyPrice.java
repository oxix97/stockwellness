package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 해외 지수 일별 시세 상세 (output2)
 */
public record KisOverseasIndexDailyPrice(
        @JsonProperty("stck_bsop_date")
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate baseDate,

        @JsonProperty("ovrs_nmix_prpr")
        BigDecimal closePrice,

        @JsonProperty("ovrs_nmix_oprc")
        BigDecimal openPrice,

        @JsonProperty("ovrs_nmix_hgpr")
        BigDecimal highPrice,

        @JsonProperty("ovrs_nmix_lwpr")
        BigDecimal lowPrice,

        @JsonProperty("prdy_vrss")
        BigDecimal prdyVrss,

        @JsonProperty("prdy_ctrt")
        BigDecimal prdyCtrt,

        @JsonProperty("acml_vol")
        Long volume
) implements BenchmarkPriceData {
}
