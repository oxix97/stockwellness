package org.stockwellness.adapter.in.kafka.marketdata;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.annotation.KafkaListener;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.MarketDataRevisionUseCase;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@ExtendWith(MockitoExtension.class)
class MarketDataRevisionConsumerTest {
    @Mock private MarketDataRevisionUseCase revisionUseCase;

    @Test
    void derived_consumer_uses_a_group_separate_from_cache_invalidation() throws Exception {
        KafkaListener listener = MarketDataRevisionConsumer.class
                .getMethod("consume", MarketDataRevisionEvent.class)
                .getAnnotation(KafkaListener.class);

        org.assertj.core.api.Assertions.assertThat(listener.groupId())
                .isEqualTo("${spring.kafka.consumer.market-data-revision-derived-group-id:market-data-revision-derived-group}");
    }

    @Test
    void revision_event_is_forwarded_to_the_recalculation_use_case() {
        var event = MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 2);
        var consumer = new MarketDataRevisionConsumer(revisionUseCase);

        consumer.consume(event);

        verify(revisionUseCase).recalculate(event);
    }
}
