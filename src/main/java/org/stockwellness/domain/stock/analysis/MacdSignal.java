package org.stockwellness.domain.stock.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum MacdSignal {
    // 0선 위/아래만 보는 것이 아니라 '추세'를 봐야 함 (단순화 버전 유지하되 명칭 구체화)
    ABOVE_ZERO("0선 상단(장기 상승 국면)"),
    BELOW_ZERO("0선 하단(장기 하락 국면)");

    private final String description;

    public static MacdSignal analyze(BigDecimal macd) {
        if (macd == null) return BELOW_ZERO;
        return macd.compareTo(BigDecimal.ZERO) > 0 ? ABOVE_ZERO : BELOW_ZERO;
    }
}