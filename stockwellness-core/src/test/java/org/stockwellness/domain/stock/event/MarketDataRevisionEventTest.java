package org.stockwellness.domain.stock.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketDataRevisionEventTest {
    @Test
    void event_id_is_stable_for_one_source_revision() {
        var first = MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 2);
        var replay = MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 2);

        assertThat(first.eventId()).isEqualTo(replay.eventId());
        assertThat(first.sourceType()).isEqualTo(MarketDataRevisionEvent.SourceType.STOCK_PRICE);
    }

    @Test
    void event_requires_a_valid_final_correction_revision_and_source_scope() {
        assertThatThrownBy(() -> MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), "KRX", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MarketDataRevisionEvent.stock(7, LocalDate.of(2026, 10, 2), " ", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
