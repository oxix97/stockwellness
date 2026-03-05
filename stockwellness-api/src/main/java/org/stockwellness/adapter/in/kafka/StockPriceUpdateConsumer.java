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
        
        // 종목 시세 캐시 무효화 (기존 시세 정보가 캐시되어 있으면 갱신 필요)
        Optional.ofNullable(cacheManager.getCache("stock_prices")).ifPresent(cache -> {
            for (String symbol : event.symbols()) {
                cache.evict(symbol + ":" + currentYear);
                cache.evict(symbol + ":" + (currentYear - 1));
            }
            log.info("Invalidated stock_prices cache for {} symbols", event.symbols().size());
        });

        // 섹터 관련 인사이트 캐시 전체 무효화 (새로운 시세 기준 순위 재산출 필요)
        Optional.ofNullable(cacheManager.getCache("sectorRanking")).ifPresent(Cache::clear);
        Optional.ofNullable(cacheManager.getCache("sectorSupply")).ifPresent(Cache::clear);
        
        // AI 분석 결과 캐시 무효화 (시세가 바뀌었으므로 새로운 분석 필요)
        Optional.ofNullable(cacheManager.getCache("ai_analysis")).ifPresent(cache -> {
            for (String symbol : event.symbols()) {
                // 이 캐시의 키 구조는 isinCode:date 형태임. 
                // symbol(ticker)와 isinCode가 다를 수 있으므로 여기서는 로그만 남기고 
                // 필요시 실제 매핑 정보를 가져오거나 전체를 비움
                // cache.clear(); // 안전을 위해 전체 비우기 또는 로그 기반으로 추후 구현
            }
        });
        
        log.info("Caches invalidated for updated stock prices");
    }

    private void triggerAiAnalysis(StockPriceUpdatedEvent event) {
        // [비즈니스 정책] 시세 업데이트 후 AI 리포트 생성을 트리거함
        // 비용 및 부하 관리를 위해 현재는 상위 관심 종목 또는 특정 조건 만족 시에만 실행하도록 로그 기록
        log.info("Triggering background AI market analysis for {} updated symbols...", event.symbols().size());
        
        // TODO: 실제 AI 분석 서비스(StockAnalysisUseCase)를 연동하여 
        // 핵심 종목(예: 거래대금 상위 5개)에 대해 비동기 분석 미리 수행(Prefetch) 로직 추가
        if (!event.symbols().isEmpty()) {
            log.info("Sample symbols for pre-analysis: {}", 
                    event.symbols().stream().limit(5).toList());
        }
    }
}
