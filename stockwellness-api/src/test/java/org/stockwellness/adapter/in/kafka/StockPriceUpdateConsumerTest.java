package org.stockwellness.adapter.in.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioAdapter;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.stockwellness.config.KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {STOCK_PRICE_UPDATED_TOPIC})
class StockPriceUpdateConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private PortfolioAdapter portfolioAdapter;

    @Test
    void testConsumeStockPriceUpdatedEvent() {
        // given
        List<String> symbols = List.of("AAPL", "TSLA");
        StockPriceUpdatedEvent event = StockPriceUpdatedEvent.of(symbols);
        List<Long> portfolioIds = List.of(1L, 2L);
        
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);
        when(portfolioAdapter.findPortfolioIdsBySymbols(symbols)).thenReturn(portfolioIds);

        // 컨슈머가 파티션을 할당받을 때까지 대기
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }

        // when
        kafkaTemplate.send(STOCK_PRICE_UPDATED_TOPIC, event);

        // then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                verify(cacheManager).getCache("sectorRanking:v3");
                verify(cacheManager).getCache("sectorSupply:v3");
                verify(cacheManager).getCache("ai_analysis:v2");
                verify(mockCache, atLeastOnce()).clear();
                verify(mockCache, atLeastOnce()).evict(anyLong());
            });
    }
}
