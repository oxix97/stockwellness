package org.stockwellness.adapter.in.web.insight;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.application.port.in.insight.MarketWeatherUseCase;
import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;
import org.stockwellness.global.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/market-weather")
@RequiredArgsConstructor
public class MarketWeatherController {

    private final MarketWeatherUseCase marketWeatherUseCase;

    @GetMapping("/latest")
    public ApiResponse<MarketWeatherResponse> getLatestWeather(
            @RequestParam(defaultValue = "KOSPI") String marketType) {
        return marketWeatherUseCase.getLatestMarketWeather(marketType)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }
}
