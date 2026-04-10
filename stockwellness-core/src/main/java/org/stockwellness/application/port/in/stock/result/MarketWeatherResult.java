package org.stockwellness.application.port.in.stock.result;

import java.time.LocalDate;

public record MarketWeatherResult(
        MarketWeatherLevel weatherLevel,
        String weatherMessage,
        String weatherDescription,
        MarketWeatherReason reasonCode,
        LocalDate asOfDate
) {
}
