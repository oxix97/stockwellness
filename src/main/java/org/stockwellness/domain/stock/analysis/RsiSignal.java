package org.stockwellness.domain.stock.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum RsiSignal {
    OVERBOUGHT(70, "과매수 구간 (70↑) - 차익 실현 매물 출회 가능성 높음"),
    OVERSOLD(30, "과매도 구간 (30↓) - 기술적 반등 가능성 높음"),
    NEUTRAL_BULLISH(50, "중립 강세 (50~70) - 매수세가 우위"),
    NEUTRAL_BEARISH(0, "중립 약세 (30~50) - 관망 심리 우세");

    private final double threshold;
    private final String description;

    public static RsiSignal analyze(BigDecimal rsi) {
        if (rsi == null) return NEUTRAL_BEARISH;
        double value = rsi.doubleValue();

        if (value >= OVERBOUGHT.threshold) return OVERBOUGHT;
        if (value <= OVERSOLD.threshold) return OVERSOLD;
        if (value >= NEUTRAL_BULLISH.threshold) return NEUTRAL_BULLISH;
        return NEUTRAL_BEARISH;
    }
}