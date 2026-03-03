package org.stockwellness.application.port.in.stock.result;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class StockPriceResultTest {

    @Test
    void createStockPriceResultWithAdvancedFields() {
        LocalDate now = LocalDate.now();
        StockPriceResult result = new StockPriceResult(
                now,
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                new BigDecimal("105"),
                1000L
        );

        assertThat(result.baseDate()).isEqualTo(now);
    }
}
