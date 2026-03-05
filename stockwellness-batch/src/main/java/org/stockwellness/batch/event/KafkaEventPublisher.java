package org.stockwellness.batch.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStockPriceUpdated(List<String> symbols) {
        StockPriceUpdatedEvent event = StockPriceUpdatedEvent.of(symbols);
        log.info("Publishing stock price updated event for symbols: {}", symbols);
        
        kafkaTemplate.send(KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published event to topic: {}, offset: {}", 
                                KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish event to topic: {}", KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, ex);
                    }
                });
    }
}
