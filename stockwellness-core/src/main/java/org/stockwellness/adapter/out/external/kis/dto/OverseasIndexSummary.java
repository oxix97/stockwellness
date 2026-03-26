package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * 해외 지수 요약 정보 (output1)
 */
public record OverseasIndexSummary(
        @JsonProperty("ovrs_nmix_prpr")
        BigDecimal currentPrice,

        @JsonProperty("prdy_vrss")
        BigDecimal prdyVrss,

        @JsonProperty("prdy_ctrt")
        BigDecimal prdyCtrt,

        @JsonProperty("ovrs_nmix_hgpr")
        BigDecimal highPrice,

        @JsonProperty("ovrs_nmix_lwpr")
        BigDecimal lowPrice
) {
}
