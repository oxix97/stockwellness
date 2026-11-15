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
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily daily = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                null, null, null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.FINAL, null);

        assertThat(daily.getSeries()).isSameAs(series);
        assertThat(daily.getId().getTradeDate()).isEqualTo(date);
        assertThat(daily.getPriceStatus()).isEqualTo(BenchmarkDataStatus.FINAL);
        assertThat(daily.getSupplyStatus()).isNull();
        assertThat(daily.getForeignNetAmount()).isNull();
    }

    @Test
    void finalPriceRequiresCloseAndFinalSupplyRequiresBothFlowsAndSource() {
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);

        assertThatThrownBy(() -> IndexDaily.of(series, date, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "API", null, BenchmarkDataStatus.FINAL, null))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                null, null, null, null, null, null, null, BigDecimal.ONE, null,
                "API", null, BenchmarkDataStatus.FINAL, BenchmarkDataStatus.FINAL))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void rejects_negative_prices_and_invalid_ohlc_ranges() {
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);

        assertThatThrownBy(() -> IndexDaily.of(series, date, null, null, null, new BigDecimal("-1"),
                null, null, null, null, null, null, null, null, null,
                "API", null, BenchmarkDataStatus.PROVISIONAL, null))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> IndexDaily.of(series, date, new BigDecimal("10"), new BigDecimal("8"),
                new BigDecimal("9"), new BigDecimal("9"), null, null, null, null, null, null, null,
                null, null, "API", null, BenchmarkDataStatus.PROVISIONAL, null))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void price_revision_changes_only_when_persisted_price_values_change() {
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily current = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500.0000"),
                10L, new BigDecimal("100.00"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);
        OffsetDateTime correctionTime = OffsetDateTime.of(2026, 10, 5, 1, 2, 3, 0, ZoneOffset.UTC);
        IndexDaily same = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                10L, new BigDecimal("100.0"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);

        assertThat(current.reviseFrom(same, correctionTime)).isFalse();
        assertThat(current.getRevision()).isEqualTo(1);
        assertThat(current.getCorrectedAt()).isNull();

        IndexDaily corrected = IndexDaily.of(series, date, null, null, null, new BigDecimal("2501"),
                10L, new BigDecimal("100.0"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null);
        assertThat(current.reviseFrom(corrected, correctionTime)).isTrue();
        assertThat(current.getRevision()).isEqualTo(2);
        assertThat(current.getCorrectedAt()).isEqualTo(correctionTime);
    }

    @Test
    void incomplete_redelivery_cannot_downgrade_final_price() {
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily current = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                10L, new BigDecimal("100"), null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.FINAL, null);
        IndexDaily incomplete = IndexDaily.of(series, date, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "FHKUP03500100", null, BenchmarkDataStatus.INCOMPLETE, null);

        assertThat(current.reviseFrom(incomplete, OffsetDateTime.now(ZoneOffset.UTC))).isFalse();
        assertThat(current.getClosePrice()).isEqualByComparingTo("2500");
        assertThat(current.getPriceStatus()).isEqualTo(BenchmarkDataStatus.FINAL);
        assertThat(current.getRevision()).isEqualTo(1);
    }

    @Test
    void supply_revision_changes_independently_and_incomplete_redelivery_cannot_downgrade_final_supply() {
        IndexSeries series = series();
        LocalDate date = LocalDate.of(2026, 10, 2);
        IndexDaily current = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                10L, new BigDecimal("100"), null, null, null, null, null,
                new BigDecimal("20"), new BigDecimal("10"), "PRICE_API", "SUPPLY_API",
                BenchmarkDataStatus.PROVISIONAL, BenchmarkDataStatus.FINAL);
        IndexDaily correction = IndexDaily.of(series, date, null, null, null, new BigDecimal("2500"),
                10L, new BigDecimal("100"), null, null, null, null, null,
                new BigDecimal("21"), new BigDecimal("10"), "PRICE_API", "SUPPLY_API",
                BenchmarkDataStatus.PROVISIONAL, BenchmarkDataStatus.FINAL);

        assertThat(current.reviseFrom(correction, OffsetDateTime.now(ZoneOffset.UTC))).isTrue();
        assertThat(current.getRevision()).isEqualTo(1);
        assertThat(current.getSupplyRevision()).isEqualTo(2);

        IndexDaily incomplete = IndexDaily.of(series, date, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "PRICE_API", "SUPPLY_API", BenchmarkDataStatus.INCOMPLETE, BenchmarkDataStatus.INCOMPLETE);
        assertThat(current.reviseFrom(incomplete, OffsetDateTime.now(ZoneOffset.UTC))).isFalse();
        assertThat(current.getSupplyStatus()).isEqualTo(BenchmarkDataStatus.FINAL);
        assertThat(current.getForeignNetAmount()).isEqualByComparingTo("21");
        assertThat(current.getSupplyRevision()).isEqualTo(2);
    }

    private static IndexSeries series() {
        return new IndexSeries(MarketIndex.of("0001", "종합", "0"), "KIS", "kis.idxcode.KOSPI", "0001",
                "KOSPI", "PRICE", "NONE", "KRW", true);
    }
}
