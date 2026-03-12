package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioDiversificationResult(
    BigDecimal totalValue,
    Map<String, BigDecimal> assetRatios,
    Map<String, BigDecimal> sectorRatios,
    Map<String, BigDecimal> countryRatios
) {}
