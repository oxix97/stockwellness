package org.stockwellness.domain.stock;

/**
 * 차트 데이터 집계 주기 (일, 주, 월)
 */
public enum ChartFrequency {
    DAILY,
    WEEKLY,
    MONTHLY;

    public static ChartFrequency fromString(String frequency) {
        try {
            return valueOf(frequency.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return DAILY;
        }
    }
}
