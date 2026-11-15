package org.stockwellness.adapter.in.kafka.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.MarketDataRevisionUseCase;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataRevisionConsumer {
    private final MarketDataRevisionUseCase revisionUseCase;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1_000, multiplier = 2.0),
            kafkaTemplate = "kafkaTemplate",
            retryTopicSuffix = "-derived-retry",
            dltTopicSuffix = "-derived-dlt"
    )
    @KafkaListener(
            topics = KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
            groupId = "${spring.kafka.consumer.market-data-revision-derived-group-id:market-data-revision-derived-group}"
    )
    public void consume(MarketDataRevisionEvent event) {
        revisionUseCase.recalculate(event);
    }

    @DltHandler
    public void handleDlt(MarketDataRevisionEvent event) {
        log.error("시장 데이터 정정 재계산 최종 실패 eventId={}", event.eventId());
    }
}
