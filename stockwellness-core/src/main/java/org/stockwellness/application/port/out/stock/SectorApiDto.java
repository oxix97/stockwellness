package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorApiDto(
        String sectorCode,
        String sectorName,
        LocalDate baseDate,
        BigDecimal sectorIndexCurrentPrice,
        BigDecimal avgFluctuationRate,
        Long netForeignBuyAmount,
        Long netInstBuyAmount
) {}
