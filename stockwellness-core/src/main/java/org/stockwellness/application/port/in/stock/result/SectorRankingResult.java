package org.stockwellness.application.port.in.stock.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SectorRankingResult(
    String sectorCode,
    String sectorName,
    BigDecimal currentPrice,
    BigDecimal fluctuationRate,
    boolean isOverheated
) implements Serializable {}
