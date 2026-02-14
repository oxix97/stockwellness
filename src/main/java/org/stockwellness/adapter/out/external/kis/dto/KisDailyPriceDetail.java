package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KisDailyPriceDetail(
        // 1. 날짜 자동 파싱 (yyyyMMdd -> LocalDate)
        @JsonProperty("stck_bsop_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
        LocalDate baseDate,

        // 2. 숫자 자동 파싱 (String "1000" -> BigDecimal 1000)
        // KIS는 모든 숫자 필드를 String으로 내려주므로 Jackson이 알아서 변환합니다.
        @JsonProperty("stck_oprc") BigDecimal openPrice,
        @JsonProperty("stck_hgpr") BigDecimal highPrice,
        @JsonProperty("stck_lwpr") BigDecimal lowPrice,
        @JsonProperty("stck_clpr") BigDecimal closePrice,

        @JsonProperty("acml_vol") Long volume,
        @JsonProperty("acml_tr_pbmn") BigDecimal transactionAmt,

        @JsonProperty("prdy_vrss") BigDecimal changeAmount,
        @JsonProperty("prdy_vrss_sign") String changeSign, // 1:상한, 2:상승, 3:보합, 4:하한, 5:하락

        @JsonProperty("flng_cls_code") String flngClsCode,
        @JsonProperty("mod_yn") String modYn
) {
    /**
     * [Quant Logic] 상한가 도달 여부 확인
     */
    public boolean isUpperLimit() {
        return "1".equals(this.changeSign);
    }

    /**
     * [Quant Logic] 하한가 도달 여부 확인
     */
    public boolean isLowerLimit() {
        return "4".equals(this.changeSign);
    }

    /**
     * [Data Quality] 거래 정지 여부 추정 (거래량이 0이고 시고저종이 모두 같음)
     */
    public boolean isSuspended() {
        return (volume == null || volume == 0) &&
                (openPrice != null && openPrice.compareTo(BigDecimal.ZERO) > 0) &&
                openPrice.compareTo(closePrice) == 0 &&
                highPrice.compareTo(lowPrice) == 0;
    }
}

        