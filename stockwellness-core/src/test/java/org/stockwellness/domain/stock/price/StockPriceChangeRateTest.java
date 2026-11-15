package org.stockwellness.domain.stock.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.Stock;

class StockPriceChangeRateTest {
    @Test
    void changeRate_is_missing_when_no_previous_final_close_exists() {
        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(1L);
        StockPrice price = StockPrice.of(stock, LocalDate.of(2026, 11, 11), new BigDecimal("100"),
                new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"), null,
                null, 100L, BigDecimal.TEN, null);

        assertThat(price.getFluctuationRate()).isNull();
    }

    @Test
    void changeRate_uses_previous_final_close() {
        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(1L);
        StockPrice price = StockPrice.of(stock, LocalDate.of(2026, 11, 11), new BigDecimal("100"),
                new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"), null,
                new BigDecimal("100"), 100L, BigDecimal.TEN, null);

        assertThat(price.getFluctuationRate()).isEqualByComparingTo("5.00");
    }
}
