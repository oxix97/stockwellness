package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MarketIndexResult(
        String name,
        BigDecimal currentPrice,
        BigDecimal fluctuationRate,
        List<HistoryPoint> history
) {
    public record HistoryPoint(
            LocalDate date,
            BigDecimal close
    ) {}
}
