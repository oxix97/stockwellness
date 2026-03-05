package org.stockwellness.adapter.in.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC})
class StockPriceUpdateConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private CacheManager cacheManager;

    @Test
    void testConsumeStockPriceUpdatedEvent() {
        // given
        List<String> symbols = List.of("AAPL", "TSLA");
        StockPriceUpdatedEvent event = StockPriceUpdatedEvent.of(symbols);
        
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        // when
        kafkaTemplate.send(KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, event);

        // then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // 캐시 무효화 메서드가 호출되었는지 확인
            verify(mockCache, atLeastOnce()).evict(anyString());
            verify(mockCache, atLeastOnce()).clear();
        });
    }
}
