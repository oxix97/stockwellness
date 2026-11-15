package org.stockwellness.adapter.out.kafka.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

class KafkaOutboxEventAdapterTest {
    @SuppressWarnings("unchecked")
    @Test
    void revision_events_are_published_with_a_stable_key_and_acknowledged_before_returning() {
        KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        var event = MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 2);
        CompletableFuture<SendResult<String, Object>> ack = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC), eq(event.eventId()), any()))
                .thenReturn(ack);
        var adapter = new KafkaOutboxEventAdapter(kafkaTemplate, objectMapper());

        adapter.publish(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                "{\"eventId\":\"" + event.eventId() + "\",\"sourceType\":\"STOCK_PRICE\","
                        + "\"sourceId\":7,\"tradeDate\":\"2026-10-02\",\"quoteScope\":\"KRX\",\"revision\":2}");

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC), eq(event.eventId()), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void kafka_failure_is_returned_to_the_outbox_relay_for_retry() {
        KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Object>> failure = new CompletableFuture<>();
        failure.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(eq("portfolio-analysis-completed"), any())).thenReturn(failure);
        var adapter = new KafkaOutboxEventAdapter(kafkaTemplate, objectMapper());

        assertThatThrownBy(() -> adapter.publish("portfolio-analysis-completed", "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not acknowledged");
    }

    private static com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
