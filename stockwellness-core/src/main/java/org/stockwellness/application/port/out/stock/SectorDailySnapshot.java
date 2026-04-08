package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorDailySnapshot(
        String indexCode,
        LocalDate baseDate,
        BigDecimal sectorIndexPrice,
        BigDecimal fluctuationRate
) {
}
