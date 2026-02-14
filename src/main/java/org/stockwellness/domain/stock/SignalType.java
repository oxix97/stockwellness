package org.stockwellness.domain.stock;

public enum SignalType {
    VOLUME_SPIKE("거래량 폭증"),
    LIQUIDITY_SURGE("거래대금 급증"),
    GOLDEN_CROSS("골든 크로스"),
    RSI_OVERSOLD("RSI 침체(매수기회)");

    private final String description;

    SignalType(String description) {
        this.description = description;
    }
}