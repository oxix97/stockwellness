package org.stockwellness.domain.stock;


public enum MarketType {
    KOSPI, KOSDAQ, KONEX, ETC;

    public static MarketType fromString(String value) {
        if (value == null) return ETC;
        try {
            return MarketType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETC;
        }
    }
}
