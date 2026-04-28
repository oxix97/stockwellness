package org.stockwellness.adapter.in.web.insight;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.web.insight.dto.MarketWeatherResponse;
import org.stockwellness.adapter.out.persistence.insight.repository.MarketWeatherRepository;
import org.stockwellness.adapter.out.persistence.insight.repository.SectorWeatherRepository;
import org.stockwellness.domain.stock.insight.WeatherState;
import org.stockwellness.global.common.SuccessResponse;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/market-weather")
@RequiredArgsConstructor
public class MarketWeatherController {

    private final MarketWeatherRepository marketWeatherRepository;
    private final SectorWeatherRepository sectorWeatherRepository;

    @GetMapping("/latest")
    public SuccessResponse<MarketWeatherResponse> getLatestWeather() {
        return marketWeatherRepository.findFirstByMarketTypeOrderByBaseDateDesc("KOSPI")
                .map(market -> {
                    var sectorWeathers = sectorWeatherRepository.findByBaseDateOrderByWeatherScoreDesc(market.getBaseDate());
                    
                    var topSectors = sectorWeathers.stream()
                            .limit(3)
                            .map(this::toSectorDto)
                            .toList();

                    var bottomSectors = sectorWeathers.stream()
                            .sorted((a, b) -> Integer.compare(a.getWeatherScore(), b.getWeatherScore()))
                            .limit(3)
                            .map(this::toSectorDto)
                            .toList();

                    WeatherState state = WeatherState.fromScore(market.getWeatherScore());

                    return SuccessResponse.success(MarketWeatherResponse.builder()
                            .baseDate(market.getBaseDate())
                            .marketType(market.getMarketType())
                            .weatherScore(market.getWeatherScore())
                            .weatherState(market.getWeatherState())
                            .weatherEmoji(state.getIconEmoji())
                            .aiSummary(market.getAiSummary())
                            .topSectors(topSectors)
                            .bottomSectors(bottomSectors)
                            .build());
                })
                .orElseGet(() -> SuccessResponse.success(null)); // Or appropriate fallback
    }

    private MarketWeatherResponse.SectorWeatherDto toSectorDto(org.stockwellness.adapter.out.persistence.insight.SectorWeatherJpaEntity entity) {
        WeatherState state = WeatherState.fromScore(entity.getWeatherScore());
        return MarketWeatherResponse.SectorWeatherDto.builder()
                .sectorCode(entity.getSectorCode())
                .score(entity.getWeatherScore())
                .state(entity.getWeatherState())
                .emoji(state.getIconEmoji())
                .aiTitle(entity.getAiTitle())
                .aiInsight(entity.getAiInsight())
                .build();
    }
}
