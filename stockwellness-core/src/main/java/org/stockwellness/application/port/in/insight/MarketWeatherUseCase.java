package org.stockwellness.application.port.in.insight;

import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;

import java.util.Optional;

public interface MarketWeatherUseCase {
    Optional<MarketWeatherResponse> getLatestMarketWeather(String marketType);
}
