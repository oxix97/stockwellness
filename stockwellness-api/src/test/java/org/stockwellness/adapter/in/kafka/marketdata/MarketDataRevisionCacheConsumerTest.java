package org.stockwellness.adapter.in.kafka.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

class MarketDataRevisionCacheConsumerTest {
    @Test
    void cache_consumer_uses_a_group_separate_from_derived_recalculation() throws Exception {
        KafkaListener listener = MarketDataRevisionCacheConsumer.class
                .getMethod("consume", MarketDataRevisionEvent.class)
                .getAnnotation(KafkaListener.class);

        assertThat(listener.groupId())
                .isEqualTo("${spring.kafka.consumer.market-data-revision-cache-group-id:market-data-revision-cache-group}");
    }

    @Test
    void revision_event_clears_market_and_portfolio_caches_without_clearing_member_cache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        var consumer = new MarketDataRevisionCacheConsumer(cacheManager);

        consumer.consume(MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 2));

        ArgumentCaptor<String> cacheNames = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(cacheManager, org.mockito.Mockito.atLeastOnce()).getCache(cacheNames.capture());
        assertThat(cacheNames.getAllValues()).contains("marketDashboard:v2", "sectorRanking:v4",
                "ai_analysis:canonical:v1", "portfolio_valuation").doesNotContain("member:v2");
        org.mockito.Mockito.verify(cache, org.mockito.Mockito.atLeastOnce()).clear();
    }
}
