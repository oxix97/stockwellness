package org.stockwellness.adapter.in.web.insight.dto;

import lombok.Builder;
import org.stockwellness.domain.stock.insight.WeatherState;

import java.time.LocalDate;
import java.util.List;

@Builder
public record MarketWeatherResponse(
    LocalDate baseDate,
    String marketType,
    int weatherScore,
    String weatherState,
    String weatherEmoji,
    String aiSummary,
    List<SectorWeatherDto> topSectors,
    List<SectorWeatherDto> bottomSectors
) {
    @Builder
    public record SectorWeatherDto(
        String sectorCode,
        String sectorName,
        int score,
        String state,
        String emoji,
        String aiTitle,
        String aiInsight
    ) {}
}
