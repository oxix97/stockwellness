package org.stockwellness.domain.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockCandle(
    LocalDate baseDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    long volume
) {}