package org.stockwellness.application.port.out.sector;

public interface WeatherInsightPort {
    String generateMarketWeatherSummary(int score, String marketType, String newsContext);
    SectorWeatherInsight generateSectorWeatherInsight(String sectorName, int score, String newsContext);
    
    record SectorWeatherInsight(String title, String insight) {}
}
