package org.stockwellness.application.service.insight;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;
import org.stockwellness.adapter.out.persistence.insight.MarketWeather;
import org.stockwellness.adapter.out.persistence.insight.repository.MarketWeatherRepository;
import org.stockwellness.application.port.in.insight.MarketWeatherUseCase;
import org.stockwellness.domain.stock.insight.WeatherState;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketWeatherService implements MarketWeatherUseCase {

    private final MarketWeatherRepository marketWeatherRepository;

    @Override
    public Optional<MarketWeatherResponse> getLatestMarketWeather(String marketType) {
        return marketWeatherRepository.findFirstByMarketTypeOrderByBaseDateDesc(marketType)
                .map(this::toResponse);
    }

    private MarketWeatherResponse toResponse(MarketWeather market) {
        WeatherState state = WeatherState.fromScore(market.getWeatherScore());

        return MarketWeatherResponse.builder()
                .baseDate(market.getBaseDate())
                .marketType(market.getMarketType())
                .weatherScore(market.getWeatherScore())
                .weatherState(market.getWeatherState())
                .weatherEmoji(state.getIconEmoji())
                .aiSummary(market.getAiSummary())
                .topSectors(toSectorDtos(market.getTopSectors()))
                .bottomSectors(toSectorDtos(market.getBottomSectors()))
                .build();
    }

    private List<MarketWeatherResponse.SectorWeatherDto> toSectorDtos(List<MarketWeather.SectorSummary> sectors) {
        if (sectors == null) return List.of();
        return sectors.stream()
                .map(s -> MarketWeatherResponse.SectorWeatherDto.builder()
                        .sectorCode(s.code())
                        .sectorName(s.name())
                        .score(s.score())
                        .emoji(s.emoji())
                        // Note: state, aiTitle, aiInsight are currently not in persistence model but in DTO
                        // They could be derived or added to persistence if needed
                        .build())
                .toList();
    }
}
