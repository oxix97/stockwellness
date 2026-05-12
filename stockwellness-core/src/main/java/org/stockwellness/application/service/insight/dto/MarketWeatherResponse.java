package org.stockwellness.application.service.insight.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

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
