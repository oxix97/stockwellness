package org.stockwellness.application.service.stock;

import java.math.BigDecimal;

public final class MarketWeatherPolicy {

    public static final int LOOKBACK_DAYS = 14;
    public static final int DISPLAY_SCALE = 2;

    public static final BigDecimal FLAT_THRESHOLD = new BigDecimal("0.15");
    public static final BigDecimal HIGH_VOLATILITY_THRESHOLD = new BigDecimal("3.00");
    public static final BigDecimal INTRADAY_SWING_THRESHOLD = new BigDecimal("4.00");

    public static final BigDecimal STORMY_KOSPI = new BigDecimal("-1.50");
    public static final BigDecimal STORMY_KOSDAQ = new BigDecimal("-1.80");
    public static final BigDecimal STORMY_DECLINE_RATIO = new BigDecimal("0.68");
    public static final BigDecimal STORMY_HIGH_VOLATILITY_RATIO = new BigDecimal("0.22");

    public static final BigDecimal CLEAR_KOSPI = new BigDecimal("1.20");
    public static final BigDecimal CLEAR_KOSDAQ = new BigDecimal("0.80");
    public static final BigDecimal CLEAR_ADVANCE_RATIO = new BigDecimal("0.62");
    public static final BigDecimal CLEAR_HIGH_VOLATILITY_RATIO_CAP = new BigDecimal("0.28");

    public static final BigDecimal SUNNY_KOSPI = new BigDecimal("0.50");
    public static final BigDecimal SUNNY_KOSDAQ = new BigDecimal("0.20");
    public static final BigDecimal SUNNY_ADVANCE_RATIO = new BigDecimal("0.55");

    public static final BigDecimal PARTLY_CLOUDY_INDEX = new BigDecimal("0.20");
    public static final BigDecimal PARTLY_CLOUDY_ADVANCE_RATIO = new BigDecimal("0.48");

    public static final BigDecimal FOGGY_KOSPI_FLOOR = new BigDecimal("-0.20");
    public static final BigDecimal FOGGY_DECLINE_RATIO = new BigDecimal("0.55");
    public static final BigDecimal FOGGY_KOSDAQ = new BigDecimal("-0.40");
    public static final BigDecimal FOGGY_ALT_DECLINE_RATIO = new BigDecimal("0.52");

    public static final BigDecimal RAINY_KOSPI = new BigDecimal("-0.50");
    public static final BigDecimal RAINY_KOSDAQ = new BigDecimal("-0.70");
    public static final BigDecimal RAINY_DECLINE_RATIO = new BigDecimal("0.58");

    public static final BigDecimal INDEX_ONLY_CLEAR_KOSPI = new BigDecimal("1.00");
    public static final BigDecimal INDEX_ONLY_CLEAR_KOSDAQ = new BigDecimal("0.70");
    public static final BigDecimal INDEX_ONLY_SUNNY_KOSPI = new BigDecimal("0.40");
    public static final BigDecimal INDEX_ONLY_PARTLY_CLOUDY_INDEX = new BigDecimal("0.10");
    public static final BigDecimal INDEX_ONLY_STORMY_KOSPI = new BigDecimal("-1.20");
    public static final BigDecimal INDEX_ONLY_STORMY_KOSDAQ = new BigDecimal("-1.50");
    public static final BigDecimal INDEX_ONLY_RAINY_KOSPI = new BigDecimal("-0.40");
    public static final BigDecimal INDEX_ONLY_RAINY_KOSDAQ = new BigDecimal("-0.60");

    private MarketWeatherPolicy() {
    }
}
