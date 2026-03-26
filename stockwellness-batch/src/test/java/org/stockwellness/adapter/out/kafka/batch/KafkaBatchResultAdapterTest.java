package org.stockwellness.adapter.out.kafka.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.stockwellness.domain.shared.event.BatchResultEvent;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

        // when
        kafkaBatchResultAdapter.send(event);

        // then
        verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(event));
    }
}
