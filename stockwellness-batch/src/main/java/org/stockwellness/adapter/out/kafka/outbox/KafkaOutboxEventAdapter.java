package org.stockwellness.adapter.out.kafka.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.outbox.OutboxEventPublisherPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventAdapter implements OutboxEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String payload) {
        log.info("Outbox 이벤트를 Kafka로 발행 중. Topic: {}", topic);
        kafkaTemplate.send(topic, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Outbox 이벤트 발행 성공. Topic: {}", topic);
                    } else {
                        log.error("Outbox 이벤트 발행 실패. Topic: {}, 사유: {}", topic, ex.getMessage());
                    }
                });
    }
}
