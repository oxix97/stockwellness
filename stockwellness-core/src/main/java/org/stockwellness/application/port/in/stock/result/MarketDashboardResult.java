package org.stockwellness.application.port.in.stock.result;

import java.util.List;

public record MarketDashboardResult(
        List<MarketIndexResult> indexes,
        MarketWeatherResult weather
) {
}
