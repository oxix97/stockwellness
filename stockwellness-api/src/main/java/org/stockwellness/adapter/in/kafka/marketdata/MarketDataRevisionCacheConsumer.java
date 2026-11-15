package org.stockwellness.adapter.in.kafka.marketdata;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataRevisionCacheConsumer {
    private static final List<String> MARKET_CACHE_NAMES = List.of(
            "ai_analysis:canonical:v1",
            "sectorRanking:v4",
            "sectorDetail:v4",
            "sectorComparison:v4",
            "marketDashboard:v2",
            "marketBreadth:v2",
            "stockSupplyRanking:v2",
            "portfolio_valuation",
            "portfolio_diversification",
            "portfolio_rebalancing");

    private final CacheManager cacheManager;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1_000, multiplier = 2.0),
            kafkaTemplate = "kafkaTemplate",
            retryTopicSuffix = "-cache-retry",
            dltTopicSuffix = "-cache-dlt"
    )
    @KafkaListener(
            topics = KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
            groupId = "${spring.kafka.consumer.market-data-revision-cache-group-id:market-data-revision-cache-group}"
    )
    public void consume(MarketDataRevisionEvent event) {
        MARKET_CACHE_NAMES.forEach(name -> Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear));
        log.info("시장 정정 캐시 무효화 완료 eventId={}", event.eventId());
    }

    @DltHandler
    public void handleDlt(MarketDataRevisionEvent event) {
        log.error("시장 데이터 정정 캐시 무효화 최종 실패 eventId={}", event.eventId());
    }
}
