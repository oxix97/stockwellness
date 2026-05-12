package org.stockwellness.application.port.in.insight;

import java.util.Optional;

import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;

public interface MarketWeatherUseCase {
    Optional<MarketWeatherResponse> getLatestMarketWeather(String marketType);
}
