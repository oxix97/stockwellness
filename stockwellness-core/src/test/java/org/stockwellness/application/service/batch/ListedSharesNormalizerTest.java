package org.stockwellness.application.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.MarketType;

class ListedSharesNormalizerTest {
    @Test
    void validates_market_units_without_inventing_a_source_date_or_persisted_value() {
        assertThat(ListedSharesNormalizer.normalize(MarketType.KOSPI, "12345").orElseThrow()).isEqualTo(12_345L);
        assertThat(ListedSharesNormalizer.normalize(MarketType.KOSDAQ, "12345").orElseThrow()).isEqualTo(12_345_000L);
        assertThat(ListedSharesNormalizer.normalize(MarketType.KOSDAQ, "  ")).isEmpty();
    }

    @Test
    void rejects_invalid_values_and_thousand_share_overflow() {
        assertThatThrownBy(() -> ListedSharesNormalizer.normalize(MarketType.KOSPI, "12x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListedSharesNormalizer.normalize(MarketType.KOSDAQ, "-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListedSharesNormalizer.normalize(MarketType.KOSDAQ, "9223372036854776"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
