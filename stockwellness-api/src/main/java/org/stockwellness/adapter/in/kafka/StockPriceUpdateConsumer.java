package org.stockwellness.adapter.in.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceUpdateConsumer {

    private final CacheManager cacheManager;

    @KafkaListener(topics = KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, groupId = "stockwellness-group")
    public void consume(StockPriceUpdatedEvent event) {
        log.info("Received stock price updated event for {} symbols", event.symbols().size());
        
        try {
            // 1. 캐시 무효화 (Redis)
            invalidateCaches(event);
            
            // 2. AI 분석 트리거 (마켓 인사이트 갱신 등)
            triggerAiAnalysis(event);
            
            log.info("Successfully processed stock price updated event");
        } catch (Exception e) {
            log.error("Error processing stock price updated event", e);
        }
    }

    private void invalidateCaches(StockPriceUpdatedEvent event) {
        int currentYear = LocalDate.now().getYear();
        
        // 종목 시세 캐시 무효화
        Optional.ofNullable(cacheManager.getCache("stock_prices")).ifPresent(cache -> {
            for (String symbol : event.symbols()) {
                cache.evict(symbol + ":" + currentYear);
                cache.evict(symbol + ":" + (currentYear - 1));
                log.debug("Evicted stock_prices cache for: {}", symbol);
            }
            log.info("Invalidated stock_prices cache for {} symbols", event.symbols().size());
        });

        // 섹터 관련 인사이트 캐시 전체 무효화
        Optional.ofNullable(cacheManager.getCache("sectorRanking")).ifPresent(Cache::clear);
        Optional.ofNullable(cacheManager.getCache("sectorSupply")).ifPresent(Cache::clear);
        
        // AI 분석 결과 캐시 무효화
        Optional.ofNullable(cacheManager.getCache("ai_analysis")).ifPresent(cache -> {
            for (String symbol : event.symbols()) {
                cache.evict(symbol);
            }
        });
        
        log.info("Caches invalidated for updated stock prices");
    }

    private void triggerAiAnalysis(StockPriceUpdatedEvent event) {
        log.info("Triggering background AI market analysis for {} updated symbols...", event.symbols().size());
        
        if (!event.symbols().isEmpty()) {
            log.info("Sample symbols for pre-analysis: {}", 
                    event.symbols().stream().limit(5).toList());
        }
    }
}
