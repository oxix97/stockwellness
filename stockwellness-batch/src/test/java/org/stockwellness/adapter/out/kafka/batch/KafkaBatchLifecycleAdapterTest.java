package org.stockwellness.adapter.out.kafka.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.stockwellness.batch.lifecycle.BatchLifecycleEvent;
import org.stockwellness.batch.lifecycle.BatchLifecycleEventType;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaBatchLifecycleAdapterTest {

    private static final String TOPIC_NAME = "batch.lifecycle";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaBatchLifecycleAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaBatchLifecycleAdapter(kafkaTemplate, TOPIC_NAME);
    }

    @Test
    @DisplayName("executionId를 Kafka key로 lifecycle 이벤트를 전송한다")
    void sendLifecycleEvent() {
        BatchLifecycleEvent event = new BatchLifecycleEvent(
                "event-1",
                BatchLifecycleEventType.STARTED,
                "stockPriceBatchJob",
                203L,
                "batch-abcd1234",
                LocalDateTime.of(2026, 4, 6, 12, 12, 31),
                null,
                "STARTING",
                "20240101",
                "20240131",
                7L,
                0L,
                0L,
                0.0d,
                null,
                null,
                null,
                null,
                null
        );
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC_NAME), eq("203"), eq(event))).thenReturn(future);

        adapter.send(event);

        verify(kafkaTemplate).send(eq(TOPIC_NAME), eq("203"), eq(event));
    }
}
