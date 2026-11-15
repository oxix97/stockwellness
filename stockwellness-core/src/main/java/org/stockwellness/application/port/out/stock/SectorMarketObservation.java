package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorMarketObservation(
        long seriesId,
        String sectorCode,
        String sectorName,
        String marketCode,
        LocalDate tradeDate,
        BigDecimal closePrice,
        BigDecimal changeRate,
        BigDecimal ma5,
        BigDecimal ma20,
        BigDecimal rsi14
) {}
