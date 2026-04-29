package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;

public record MarketWeatherPolicy(
    BigDecimal trendWeight,
    BigDecimal breadthWeight,
    BigDecimal momentumWeight,
    int rollingWindowDays
) {
    public static final MarketWeatherPolicy DEFAULT = new MarketWeatherPolicy(
        new BigDecimal("0.4"),
        new BigDecimal("0.4"),
        new BigDecimal("0.2"),
        252
    );
}
