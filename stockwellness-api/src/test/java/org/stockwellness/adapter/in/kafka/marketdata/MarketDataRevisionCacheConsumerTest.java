package org.stockwellness.adapter.in.kafka.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class MarketDataRevisionCacheConsumerTest {
    private static final Duration CONSUMER_TIMEOUT = Duration.ofSeconds(15);

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;
    @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean private CacheManager cacheManager;

    @Test
    void cache_consumer_uses_a_group_separate_from_derived_recalculation() throws Exception {
        KafkaListener listener = MarketDataRevisionCacheConsumer.class
                .getMethod("consume", MarketDataRevisionEvent.class)
                .getAnnotation(KafkaListener.class);

        assertThat(listener.groupId())
                .isEqualTo("${spring.kafka.consumer.market-data-revision-cache-group-id:market-data-revision-cache-group}");
        assertThat(listener.topics()).containsExactly(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC);
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

    @Test
    void duplicate_revision_after_consumer_restart_clears_the_market_caches_again() throws Exception {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        MessageListenerContainer listener = listenerRegistry.getListenerContainers().stream()
                .filter(container -> {
                    String[] topics = container.getContainerProperties().getTopics();
                    return topics != null && List.of(topics).contains(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC);
                })
                .findFirst()
                .orElseThrow();
        MarketDataRevisionEvent event = MarketDataRevisionEvent.stock(
                7, LocalDate.of(2026, 10, 2), "KRX", 2);

        listener.start();
        awaitAssignment(listener);
        kafkaTemplate.send(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event.eventId(), event)
                .get(CONSUMER_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> verify(cache, times(10)).clear());

        listener.stop();
        kafkaTemplate.send(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event.eventId(), event)
                .get(CONSUMER_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        verify(cache, times(10)).clear();

        listener.start();
        awaitAssignment(listener);
        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> verify(cache, times(20)).clear());
    }

    private void awaitAssignment(MessageListenerContainer listener) {
        Awaitility.await().atMost(CONSUMER_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> listener.getAssignedPartitions().size() == embeddedKafkaBroker.getPartitionsPerTopic());
    }
}
