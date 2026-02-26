package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;

public record SectorRankingResult(
    String sectorCode,
    String sectorName,
    BigDecimal currentPrice,
    BigDecimal fluctuationRate,
    boolean isOverheated
) {}
