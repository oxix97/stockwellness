package org.stockwellness.domain.stock.insight;

import java.time.LocalDate;

public record SectorWeather(
    String sectorCode,
    LocalDate baseDate,
    int weatherScore,
    WeatherState state,
    String aiTitle,
    String aiInsight
) {
    public static SectorWeather of(String sectorCode, LocalDate baseDate, int score, String aiTitle, String aiInsight) {
        return new SectorWeather(sectorCode, baseDate, score, WeatherState.fromScore(score), aiTitle, aiInsight);
    }
}
