package org.stockwellness.domain.stock.insight;

import java.time.LocalDate;

public record MarketWeather(
    String marketType,
    LocalDate baseDate,
    int weatherScore,
    WeatherState state,
    String aiSummary,
    String topSectorsJson,
    String bottomSectorsJson
) {
    public static MarketWeather of(String marketType, LocalDate baseDate, int score, String aiSummary, String topSectorsJson, String bottomSectorsJson) {
        return new MarketWeather(marketType, baseDate, score, WeatherState.fromScore(score), aiSummary, topSectorsJson, bottomSectorsJson);
    }
}
