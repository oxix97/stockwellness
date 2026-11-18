package org.stockwellness.adapter.in.kafka.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.consumer.market-data-revision-cache-group-id=market-data-revision-cache-redis-test"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class MarketDataRevisionRedisCacheIntegrationTest {
    private static final Duration CONSUMER_TIMEOUT = Duration.ofSeconds(30);
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;
    @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired private CacheManager cacheManager;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        REDIS.start();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("stockwellness.cache.namespace", () -> "revision-cache-integration-test");
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @Test
    void revision_event_clears_market_and_portfolio_values_in_redis_only() throws Exception {
        Cache marketCache = cacheManager.getCache("marketDashboard:v2");
        Cache portfolioCache = cacheManager.getCache("portfolio_valuation");
        Cache memberCache = cacheManager.getCache("member:v2");
        assertThat(marketCache).isNotNull();
        assertThat(portfolioCache).isNotNull();
        assertThat(memberCache).isNotNull();
        marketCache.put("revision-test", "market-value");
        portfolioCache.put("revision-test", "portfolio-value");
        memberCache.put("revision-test", "member-value");

        MessageListenerContainer listener = listenerRegistry.getListenerContainers().stream()
                .filter(container -> {
                    String[] topics = container.getContainerProperties().getTopics();
                    return topics != null && List.of(topics).contains(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC);
                })
                .findFirst()
                .orElseThrow();
        listener.start();
        awaitAssignment(listener);
        MarketDataRevisionEvent event = MarketDataRevisionEvent.stock(
                7, LocalDate.of(2026, 10, 2), "KRX", 2);
        kafkaTemplate.send(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event.eventId(), event)
                .get(CONSUMER_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> {
            assertThat(marketCache.get("revision-test")).isNull();
            assertThat(portfolioCache.get("revision-test")).isNull();
        });
        assertThat(memberCache.get("revision-test").get()).isEqualTo("member-value");
        listener.stop();
    }

    private void awaitAssignment(MessageListenerContainer listener) {
        Awaitility.await().atMost(CONSUMER_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> listener.getAssignedPartitions().size() == embeddedKafkaBroker.getPartitionsPerTopic());
    }
}
