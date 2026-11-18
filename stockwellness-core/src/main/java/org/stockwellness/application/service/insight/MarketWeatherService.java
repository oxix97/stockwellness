package org.stockwellness.application.service.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.insight.MarketWeatherUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;
import org.stockwellness.domain.stock.insight.WeatherState;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketWeatherService implements MarketWeatherUseCase {
    private final IndexDailyReadPort indexDailyReadPort;

    @Override
    public Optional<MarketWeatherResponse> getLatestMarketWeather(String marketType) {
        String publicCode = switch (marketType) {
            case "KOSPI" -> "0001";
            case "KOSDAQ" -> "1001";
            default -> null;
        };
        if (publicCode == null) return Optional.empty();

        return indexDailyReadPort.latestWeatherDate(publicCode).flatMap(date -> {
            List<MarketWeatherResponse.SectorWeatherDto> sectors = indexDailyReadPort
                    .finalSectorWeatherScores(publicCode, date).stream()
                    .map(sector -> {
                        int sectorScore = sector.score().intValue();
                        WeatherState sectorState = WeatherState.fromScore(sectorScore);
                        return MarketWeatherResponse.SectorWeatherDto.builder()
                                .sectorCode(sector.code())
                                .sectorName(sector.name())
                                .score(sectorScore)
                                .state(sectorState.getStateName())
                                .emoji(sectorState.getIconEmoji())
                                .build();
                    }).toList();
            if (sectors.isEmpty()) return Optional.empty();

            BigDecimal sum = sectors.stream()
                    .map(sector -> BigDecimal.valueOf(sector.score()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int value = sum.divide(BigDecimal.valueOf(sectors.size()), 0, RoundingMode.HALF_UP).intValueExact();
            WeatherState state = WeatherState.fromScore(value);
            Comparator<MarketWeatherResponse.SectorWeatherDto> byScore =
                    Comparator.comparingInt(MarketWeatherResponse.SectorWeatherDto::score)
                            .thenComparing(MarketWeatherResponse.SectorWeatherDto::sectorCode);
            Comparator<MarketWeatherResponse.SectorWeatherDto> highestFirst =
                    Comparator.comparingInt(MarketWeatherResponse.SectorWeatherDto::score)
                            .reversed()
                            .thenComparing(MarketWeatherResponse.SectorWeatherDto::sectorCode);
            List<MarketWeatherResponse.SectorWeatherDto> topSectors = sectors.stream()
                    .sorted(highestFirst).limit(3).toList();
            List<MarketWeatherResponse.SectorWeatherDto> bottomSectors = sectors.stream()
                    .sorted(byScore).limit(3).toList();
            return Optional.of(MarketWeatherResponse.builder()
                    .baseDate(date)
                    .marketType(marketType)
                    .weatherScore(value)
                    .weatherState(state.getStateName())
                    .weatherEmoji(state.getIconEmoji())
                    // shortcut: AI narratives are not wired to index_ai_analysis yet; migrate the async insight flow before exposing them.
                    .topSectors(topSectors)
                    .bottomSectors(bottomSectors)
                    .build());
        });
    }
}
