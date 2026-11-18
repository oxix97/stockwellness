package org.stockwellness.adapter.out.kafka.outbox;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.outbox.OutboxEventPublisherPort;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventAdapter implements OutboxEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, String payload) {
        log.info("Outbox 이벤트를 Kafka로 발행 중. Topic: {}", topic);
        try {
            if (KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC.equals(topic)
                    || KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC.equals(topic)) {
                MarketDataRevisionEvent event = objectMapper.readValue(payload, MarketDataRevisionEvent.class);
                kafkaTemplate.send(topic, event.eventId(), event).get(10, TimeUnit.SECONDS);
            } else {
                kafkaTemplate.send(topic, payload).get(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event", ex);
        } catch (ExecutionException | TimeoutException | JsonProcessingException ex) {
            throw new IllegalStateException("Outbox event was not acknowledged by Kafka", ex);
        }
    }
}
