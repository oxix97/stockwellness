package org.stockwellness.adapter.out.kafka.batch;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.stockwellness.domain.shared.event.BatchResultEvent;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaBatchResultAdapterTest {

    private static final String TOPIC_NAME = "test-topic";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaBatchResultAdapter kafkaBatchResultAdapter;

    @BeforeEach
    void setUp() {
        kafkaBatchResultAdapter = new KafkaBatchResultAdapter(kafkaTemplate, TOPIC_NAME);
    }

    @Test
    @DisplayName("배치 결과 이벤트를 Kafka로 전송한다")
    void sendBatchResultEvent() {
        // given
        BatchResultEvent event = BatchResultEvent.success("test-batch", 100L, 500L);
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC_NAME), eq(event))).thenReturn(future);

        // when
        kafkaBatchResultAdapter.send(event);

        // then
        verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(event));
    }

    @Test
    @DisplayName("Kafka 전송 실패 시 예외가 발생해도 로직이 중단되지 않고 로깅된다")
    void sendBatchResultEventFailure() {
        // given
        BatchResultEvent event = BatchResultEvent.success("test-batch", 100L, 500L);
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection failed"));
        
        when(kafkaTemplate.send(eq(TOPIC_NAME), eq(event))).thenReturn(future);

        // when
        kafkaBatchResultAdapter.send(event);

        // then
        verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(event));
    }
}
