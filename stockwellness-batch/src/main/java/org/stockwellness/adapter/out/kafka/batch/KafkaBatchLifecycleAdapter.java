package org.stockwellness.adapter.out.kafka.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.batch.support.lifecycle.BatchLifecycleEvent;
import org.stockwellness.batch.support.lifecycle.BatchLifecycleEventPort;

@Slf4j
@Component
public class KafkaBatchLifecycleAdapter implements BatchLifecycleEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public KafkaBatchLifecycleAdapter(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topic.batch-lifecycle:batch.lifecycle}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public void send(BatchLifecycleEvent event) {
        String key = event.executionId() == null ? null : String.valueOf(event.executionId());
        try {
            var future = kafkaTemplate.send(topicName, key, event);
            if (future == null) {
                log.error("배치 lifecycle 이벤트 전송 실패 - job={}, eventType={}, executionId={}, 사유=KafkaTemplate returned null future",
                        event.jobName(), event.eventType(), event.executionId());
                return;
            }

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("배치 lifecycle 이벤트 전송 성공 - job={}, eventType={}, executionId={}",
                            event.jobName(), event.eventType(), event.executionId());
                } else {
                    log.error("배치 lifecycle 이벤트 전송 실패 - job={}, eventType={}, executionId={}, 사유={}",
                            event.jobName(), event.eventType(), event.executionId(), ex.getMessage());
                }
            });
        } catch (Exception exception) {
            log.error("배치 lifecycle 이벤트 전송 실패 - job={}, eventType={}, executionId={}, 사유={}",
                    event.jobName(), event.eventType(), event.executionId(), exception.getMessage(), exception);
        }
    }
}
