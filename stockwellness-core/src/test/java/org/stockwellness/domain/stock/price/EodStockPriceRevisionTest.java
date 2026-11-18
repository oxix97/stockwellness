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

    private static EodStockPrice quote(Stock stock, LocalDate date, String close) {
        return EodStockPrice.of(stock, date, "KRX", new BigDecimal("2500"), new BigDecimal("2600"),
                new BigDecimal("2400"), new BigDecimal(close), 1L, new BigDecimal("1.00"),
                "KIS:FHKST03010100:RAW", "PROVISIONAL");
    }
}
