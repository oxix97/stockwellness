package org.stockwellness.adapter.in.kafka.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.stockwellness.adapter.out.persistence.outbox.OutboxEventRepository;
import org.stockwellness.application.port.in.batch.MarketDataRevisionUseCase;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.consumer.market-data-revision-derived-group-id=market-data-revision-derived-restart-test"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class MarketDataRevisionConsumerKafkaTest {
    private static final Duration CONSUMER_TIMEOUT = Duration.ofSeconds(45);

    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;
    @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired private OutboxPort outboxPort;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private MarketDataRevisionUseCase revisionUseCase;

    @Test
    void outbox_events_are_published_and_replayed_after_the_derived_consumer_restarts() throws Exception {
        MessageListenerContainer listener = listenerRegistry.getListenerContainers().stream()
                .filter(container -> {
                    String[] topics = container.getContainerProperties().getTopics();
                    return topics != null && List.of(topics).contains(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
                })
                .findFirst()
                .orElseThrow();
        MarketDataRevisionEvent event = MarketDataRevisionEvent.stock(
                7, LocalDate.of(2026, 10, 2), "KRX", 2);
        String payload = objectMapper.writeValueAsString(event);

        listener.start();
        awaitAssignment(listener);
        outboxPort.save(OutboxEvent.create(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, payload));
        awaitCompletedOutboxPayload(payload, 1);
        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> verify(revisionUseCase).recalculate(event));

        listener.stop();
        clearInvocations(revisionUseCase);
        outboxPort.save(OutboxEvent.create(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, payload));
        awaitCompletedOutboxPayload(payload, 2);

        listener.start();
        awaitAssignment(listener);
        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> verify(revisionUseCase).recalculate(event));
        verify(revisionUseCase, times(1)).recalculate(event);
    }

    private void awaitCompletedOutboxPayload(String payload, long expectedCount) {
        Awaitility.await().atMost(CONSUMER_TIMEOUT).untilAsserted(() -> assertThat(
                outboxEventRepository.findAllByStatus(OutboxStatus.COMPLETED).stream()
                        .filter(event -> payload.equals(event.getPayload())).count()).isEqualTo(expectedCount));
    }

    private void awaitAssignment(MessageListenerContainer listener) {
        Awaitility.await().atMost(CONSUMER_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> listener.getAssignedPartitions().size() == embeddedKafkaBroker.getPartitionsPerTopic());
    }
}
