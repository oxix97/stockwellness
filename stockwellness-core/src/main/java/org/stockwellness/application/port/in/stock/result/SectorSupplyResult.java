package org.stockwellness.application.port.in.stock.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SectorSupplyResult(
    String sectorCode,
    String sectorName,
    Long netForeignBuyAmount,
    Long netInstBuyAmount,
    Integer foreignConsecutiveBuyDays,
    Integer instConsecutiveBuyDays
) implements Serializable {}
