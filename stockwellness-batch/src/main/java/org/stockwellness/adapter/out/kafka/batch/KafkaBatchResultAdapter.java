package org.stockwellness.adapter.out.kafka.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.batch.BatchResultEventPort;
import org.stockwellness.domain.shared.event.BatchResultEvent;

/**
 * 배치 작업 결과를 Kafka 토픽으로 발행하는 어댑터
 */
@Slf4j
@Component
public class KafkaBatchResultAdapter implements BatchResultEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public KafkaBatchResultAdapter(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${spring.kafka.template.default-topic:price-indicator-batch-result}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public void send(BatchResultEvent event) {
        log.info("배치 결과 이벤트를 Kafka로 전송 중: {}", event);
        try {
            var future = kafkaTemplate.send(topicName, event);
            if (future == null) {
                log.error("배치 결과 이벤트 전송 실패 - 작업명: [{}], 사유: KafkaTemplate returned null future", event.batchName());
                return;
            }

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("배치 결과 이벤트 전송 성공 - 작업명: [{}]", event.batchName());
                } else {
                    log.error("배치 결과 이벤트 전송 실패 - 작업명: [{}], 사유: {}",
                            event.batchName(), ex.getMessage());
                }
            });
        } catch (Exception exception) {
            log.error("배치 결과 이벤트 전송 실패 - 작업명: [{}], 사유: {}", event.batchName(), exception.getMessage(), exception);
        }
    }
}
