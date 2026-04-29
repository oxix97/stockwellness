package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;

public record MarketWeatherIndicatorSet(
    BigDecimal ma20Disparity,
    BigDecimal adr,
    BigDecimal rsi14
) {
}
