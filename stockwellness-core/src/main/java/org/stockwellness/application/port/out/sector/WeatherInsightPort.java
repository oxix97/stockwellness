package org.stockwellness.application.port.out.sector;

import org.stockwellness.domain.stock.insight.MarketWeather;
import org.stockwellness.domain.stock.insight.SectorWeather;

public interface WeatherInsightPort {
    String generateMarketWeatherSummary(int score, String marketType, String newsContext);
    SectorWeatherInsight generateSectorWeatherInsight(String sectorName, int score, String newsContext);
    
    record SectorWeatherInsight(String title, String insight) {}
}
