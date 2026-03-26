package org.stockwellness.adapter.out.kafka.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.batch.BatchResultEventPort;
import org.stockwellness.domain.shared.event.BatchResultEvent;

@Component
public class KafkaBatchResultAdapter implements BatchResultEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaBatchResultAdapter.class);

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
        log.info("Sending batch result event to Kafka: {}", event);
        kafkaTemplate.send(topicName, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent batch result event for job [{}]", event.batchName());
                    } else {
                        log.error("Failed to send batch result event for job [{}]: {}", 
                                 event.batchName(), ex.getMessage());
                    }
                });
    }
}
