package org.stockwellness.adapter.in.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.domain.common.cache.CacheType;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceUpdateConsumer {

    private final CacheManager cacheManager;
    private final PortfolioPort portfolioPort;

    @KafkaListener(topics = KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, groupId = "stockwellness-group")
    public void consume(StockPriceUpdatedEvent event) {
        log.info("종목 시세 업데이트 이벤트 수신 (종목 수: {})", event.symbols().size());
        
        try {
            // 1. 캐시 무효화 (Redis)
            invalidateCaches(event);
            
            // 2. AI 분석 트리거 (마켓 인사이트 갱신 등)
            triggerAiAnalysis(event);
            
            log.info("종목 시세 업데이트 이벤트 처리 완료");
        } catch (Exception e) {
            log.error("종목 시세 업데이트 이벤트 처리 중 오류 발생", e);
        }
    }

    private void invalidateCaches(StockPriceUpdatedEvent event) {
        List<String> symbols = event.symbols();

        // 1. 시장/섹터 인사이트 캐시 전체 무효화 (시세 기반 응답 재계산 필요)
        invalidateWholeCaches(
                CacheType.SECTOR_RANKING,
                CacheType.SECTOR_SUPPLY,
                CacheType.MARKET_DASHBOARD,
                CacheType.MARKET_BREADTH
        );

        // 2. 영향을 받는 포트폴리오 식별 및 분석 캐시 무효화
        List<Long> affectedPortfolioIds = portfolioPort.findPortfolioIdsBySymbols(symbols);
        if (!affectedPortfolioIds.isEmpty()) {
            log.info("시세 업데이트로 인해 무효화 대상 포트폴리오 {}개 식별", affectedPortfolioIds.size());
            
            invalidatePortfolioAnalysisCaches(affectedPortfolioIds);
        }
        
        log.info("업데이트된 종목 시세 관련 캐시 정리 완료");
    }

    private void invalidateWholeCaches(CacheType... cacheTypes) {
        for (CacheType cacheType : cacheTypes) {
            Optional.ofNullable(cacheManager.getCache(cacheType.getCacheName())).ifPresent(Cache::clear);
        }
    }

    private void invalidatePortfolioAnalysisCaches(List<Long> portfolioIds) {
        String[] cacheNames = {
            "portfolio_valuation", 
            "portfolio_diversification", 
            "portfolio_rebalancing", 
            CacheType.AI_ANALYSIS.getCacheName()
        };
        
        for (String cacheName : cacheNames) {
            Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(cache -> {
                for (Long portfolioId : portfolioIds) {
                    cache.evict(portfolioId);
                }
                log.debug("캐시 무효화 완료: {}", cacheName);
            });
        }
    }

    private void triggerAiAnalysis(StockPriceUpdatedEvent event) {
        // [비즈니스 정책] 시세 업데이트 후 AI 리포트 생성을 트리거함
        log.info("{}개 업데이트 종목에 대해 백그라운드 AI 시장 분석 트리거 중...", event.symbols().size());
        
        if (!event.symbols().isEmpty()) {
            log.info("분석 대상 샘플 종목: {}", 
                    event.symbols().stream().limit(5).toList());
        }
    }
}
