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
    private static final int MAX_BATCH_SIZE = 500; // Kafka 메시지 당 최대 심볼 수 제한

    public void publishStockPriceUpdated(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return;

        // 분할 발행 로직 추가 (메시지 크기 제한 및 컨슈머 부하 분산)
        for (int i = 0; i < symbols.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, symbols.size());
            List<String> subList = symbols.subList(i, end);
            
            StockPriceUpdatedEvent event = StockPriceUpdatedEvent.of(subList);
            log.info("Publishing stock price updated event for {}/{} symbols", subList.size(), symbols.size());
            
            kafkaTemplate.send(KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published chunk to topic: {}, offset: {}", 
                                    KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish chunk to topic: {}", KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, ex);
                        }
                    });
        }
    }
}
