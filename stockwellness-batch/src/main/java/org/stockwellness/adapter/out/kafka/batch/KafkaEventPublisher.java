package org.stockwellness.adapter.out.kafka.batch;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStockPriceUpdated(List<String> symbols) {
        StockPriceUpdatedEvent event = StockPriceUpdatedEvent.of(symbols);
        log.info("종목 시세 업데이트 이벤트 발행 중 (종목 수: {})", symbols.size());
        
        kafkaTemplate.send(KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("이벤트 발행 성공: 토픽={}, 오프셋={}", 
                                KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, result.getRecordMetadata().offset());
                    } else {
                        log.error("이벤트 발행 실패: 토픽={}", KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC, ex);
                    }
                });
    }
}
