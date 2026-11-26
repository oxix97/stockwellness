package org.stockwellness.domain.stock.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;

class EodStockPriceRevisionTest {
    @Test
    void same_observation_keeps_revision_and_correction_increments_it() {
        Stock stock = Stock.of("005930", "KR7005930003", "Samsung", MarketType.KOSPI,
                Currency.KRW, StockSector.empty(), StockStatus.ACTIVE);
        LocalDate date = LocalDate.of(2026, 10, 2);
        EodStockPrice existing = quote(stock, date, "2500.0000");
        Instant now = Instant.parse("2026-10-05T00:00:00Z");

        assertThat(existing.reviseFrom(quote(stock, date, "2500"), now)).isFalse();
        assertThat(existing.getRevision()).isEqualTo(1);
        assertThat(existing.getCorrectedAt()).isNull();

        assertThat(existing.reviseFrom(quote(stock, date, "2501"), now)).isTrue();
        assertThat(existing.getRevision()).isEqualTo(2);
        assertThat(existing.getCorrectedAt()).isEqualTo(now);
    }

    @Test
    void provisional_reread_close_change_increments_revision_without_promoting_to_final() {
        Stock stock = Stock.of("005930", "KR7005930003", "Samsung", MarketType.KOSPI,
                Currency.KRW, StockSector.empty(), StockStatus.ACTIVE);
        LocalDate date = LocalDate.of(2026, 11, 12);
        // Synthetic reproduction of the 2026-11-12 captured 206500 quote and 2026-11-13 reread 208000 observation
        EodStockPrice existing = EodStockPrice.of(stock, date, "KRX",
                new BigDecimal("208500"), new BigDecimal("214000"), new BigDecimal("206000"), new BigDecimal("206500"),
                19721557L, new BigDecimal("4126896638615.00"), "KIS:FHKST03010100:RAW", "PROVISIONAL");
        EodStockPrice syntheticReread208000 = EodStockPrice.of(stock, date, "KRX",
                new BigDecimal("208500"), new BigDecimal("214000"), new BigDecimal("206000"), new BigDecimal("208000"),
                19721557L, new BigDecimal("4126896638615.00"), "KIS:FHKST03010100:RAW", "PROVISIONAL");
        Instant rereadAt = Instant.parse("2026-11-13T01:00:00Z");

        assertThat(existing.reviseFrom(syntheticReread208000, rereadAt)).isTrue();
        assertThat(existing.getDataStatus()).isEqualTo("PROVISIONAL");
        assertThat(existing.getRevision()).isEqualTo(2);
        assertThat(existing.getClosePrice()).isEqualByComparingTo("208000");
        assertThat(existing.getCorrectedAt()).isEqualTo(rereadAt);

        assertThat(existing.reviseFrom(syntheticReread208000, rereadAt.plusSeconds(60))).isFalse();
        assertThat(existing.getDataStatus()).isEqualTo("PROVISIONAL");
        assertThat(existing.getRevision()).isEqualTo(2);
    }

    private static EodStockPrice quote(Stock stock, LocalDate date, String close) {
        return EodStockPrice.of(stock, date, "KRX", new BigDecimal("2500"), new BigDecimal("2600"),
                new BigDecimal("2400"), new BigDecimal(close), 1L, new BigDecimal("1.00"),
                "KIS:FHKST03010100:RAW", "PROVISIONAL");
    }
}
