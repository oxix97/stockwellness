package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.exception.GlobalException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexDailyTest {
    @Test
    void storesPriceAndSupplyAvailabilityIndependently() {
        MarketIndex index = MarketIndex.of("0001", "종합");
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily daily = IndexDaily.of(index, date, null, null, null, new BigDecimal("2500"),
                null, null, null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.FINAL, null);

        assertThat(daily.getId().getTradeDate()).isEqualTo(date);
        assertThat(daily.getPriceStatus()).isEqualTo(BenchmarkDataStatus.FINAL);
        assertThat(daily.getSupplyStatus()).isNull();
        assertThat(daily.getForeignNetAmount()).isNull();
    }

    @Test
    void finalPriceRequiresCloseAndFinalSupplyRequiresBothFlowsAndSource() {
        MarketIndex index = MarketIndex.of("0001", "종합");
        LocalDate date = LocalDate.of(2026, 10, 2);

        assertThatThrownBy(() -> IndexDaily.of(index, date, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "API", null, BenchmarkDataStatus.FINAL, null))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> IndexDaily.of(index, date, null, null, null, new BigDecimal("2500"),
                null, null, null, null, null, null, null, BigDecimal.ONE, null,
                "API", null, BenchmarkDataStatus.FINAL, BenchmarkDataStatus.FINAL))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void price_revision_changes_only_when_persisted_price_values_change() {
        MarketIndex index = MarketIndex.of("0001", "종합");
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily current = IndexDaily.of(index, date, null, null, null, new BigDecimal("2500.0000"),
                10L, new BigDecimal("100.00"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);
        OffsetDateTime correctionTime = OffsetDateTime.of(2026, 10, 5, 1, 2, 3, 0, ZoneOffset.UTC);
        IndexDaily same = IndexDaily.of(index, date, null, null, null, new BigDecimal("2500"),
                10L, new BigDecimal("100.0"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);

        assertThat(current.reviseFrom(same, correctionTime)).isFalse();
        assertThat(current.getDataRevision()).isEqualTo(1);
        assertThat(current.getCorrectedAt()).isNull();

        IndexDaily corrected = IndexDaily.of(index, date, null, null, null, new BigDecimal("2501"),
                10L, new BigDecimal("100.0"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);
        assertThat(current.reviseFrom(corrected, correctionTime)).isTrue();
        assertThat(current.getDataRevision()).isEqualTo(2);
        assertThat(current.getCorrectedAt()).isEqualTo(correctionTime);
    }
}
