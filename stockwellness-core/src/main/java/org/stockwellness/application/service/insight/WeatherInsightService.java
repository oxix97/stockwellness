package org.stockwellness.application.service.insight;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.insight.MarketWeatherJpaEntity;
import org.stockwellness.adapter.out.persistence.insight.SectorWeatherJpaEntity;
import org.stockwellness.adapter.out.persistence.insight.repository.MarketWeatherRepository;
import org.stockwellness.adapter.out.persistence.insight.repository.SectorWeatherRepository;
import org.stockwellness.application.port.out.external.SearchApiPort;
import org.stockwellness.application.port.out.messaging.MarketScoreCalculatedEvent;
import org.stockwellness.application.port.out.sector.WeatherInsightPort;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherInsightService {

    private final SearchApiPort searchApiPort;
    private final WeatherInsightPort weatherInsightPort;
    private final MarketWeatherRepository marketWeatherRepository;
    private final SectorWeatherRepository sectorWeatherRepository;

    @Transactional
    public void generateInsights(MarketScoreCalculatedEvent event) {
        log.info("🤖 Generating AI Insights for market: {} on {}", event.marketType(), event.baseDate());

        // 1. Search News via Tavily
        String newsContext = searchApiPort.searchFinancialNews(event.marketType() + " 시장 현황 및 주요 경제 지표");

        // 2. Generate Market Summary via OpenAI
        String summary = weatherInsightPort.generateMarketWeatherSummary(event.overallScore(), event.marketType(), newsContext);

        // 3. Save Market Weather
        MarketWeatherJpaEntity marketWeather = MarketWeatherJpaEntity.builder()
                .baseDate(event.baseDate())
                .marketType(event.marketType())
                .weatherScore(event.overallScore())
                .weatherState(determineState(event.overallScore()))
                .aiSummary(summary)
                .build();
        marketWeatherRepository.save(marketWeather);

        // 4. Generate Top/Bottom Sector Insights (Limited to top 2 for cost optimization)
        // For demonstration, processing top 2 if available
        List<MarketScoreCalculatedEvent.SectorScore> topSectors = event.sectorScores().stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(2)
                .toList();

        for (MarketScoreCalculatedEvent.SectorScore sector : topSectors) {
            String sectorNews = searchApiPort.searchFinancialNews(sector.name() + " 업종 최근 동향");
            var insight = weatherInsightPort.generateSectorWeatherInsight(sector.name(), sector.score(), sectorNews);

            SectorWeatherJpaEntity sectorWeather = SectorWeatherJpaEntity.builder()
                    .baseDate(event.baseDate())
                    .sectorCode(sector.code())
                    .weatherScore(sector.score())
                    .weatherState(determineState(sector.score()))
                    .aiTitle(insight.title())
                    .aiInsight(insight.insight())
                    .build();
            sectorWeatherRepository.save(sectorWeather);
        }
        
        log.info("✅ Finished generating AI Insights");
    }

    private String determineState(int score) {
        if (score >= 80) return "SUNNY";
        if (score >= 60) return "PARTLY_CLOUDY";
        if (score >= 40) return "CLOUDY";
        if (score >= 20) return "RAINY";
        return "STORMY";
    }
}
