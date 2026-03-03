package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SectorComparisonResult(
    String sectorCode,
    String sectorName,
    LocalDate baseDate,
    BigDecimal sectorChangeRate,
    BigDecimal marketChangeRate,
    BigDecimal relativeStrength,
    String performanceStatus, // "OUTPERFORM", "UNDERPERFORM", "NEUTRAL"
    List<HistoricalRS> historicalComparison
) {
    public record HistoricalRS(
        LocalDate date,
        BigDecimal relativeStrength
    ) {}
}
