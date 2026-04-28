package org.stockwellness.adapter.in.web.insight;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.web.insight.dto.MarketWeatherResponse;
import org.stockwellness.adapter.out.persistence.insight.MarketWeatherJpaEntity;
import org.stockwellness.adapter.out.persistence.insight.repository.MarketWeatherRepository;
import org.stockwellness.domain.stock.insight.WeatherState;
import org.stockwellness.global.common.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-weather")
@RequiredArgsConstructor
public class MarketWeatherController {

    private final MarketWeatherRepository marketWeatherRepository;

    @GetMapping("/latest")
    public ApiResponse<MarketWeatherResponse> getLatestWeather() {
        return marketWeatherRepository.findFirstByMarketTypeOrderByBaseDateDesc("KOSPI")
                .map(market -> {
                    WeatherState state = WeatherState.fromScore(market.getWeatherScore());

                    return ApiResponse.success(MarketWeatherResponse.builder()
                            .baseDate(market.getBaseDate())
                            .marketType(market.getMarketType())
                            .weatherScore(market.getWeatherScore())
                            .weatherState(market.getWeatherState())
                            .weatherEmoji(state.getIconEmoji())
                            .aiSummary(market.getAiSummary())
                            .topSectors(toSectorDtos(market.getTopSectors()))
                            .bottomSectors(toSectorDtos(market.getBottomSectors()))
                            .build());
                })
                .orElseGet(() -> ApiResponse.success(null));
    }

    private List<MarketWeatherResponse.SectorWeatherDto> toSectorDtos(List<MarketWeatherJpaEntity.SectorSummary> sectors) {
        if (sectors == null) return List.of();
        return sectors.stream()
                .map(s -> MarketWeatherResponse.SectorWeatherDto.builder()
                        .sectorCode(s.code())
                        .sectorName(s.name())
                        .score(s.score())
                        .emoji(s.emoji())
                        .build())
                .toList();
    }
}
