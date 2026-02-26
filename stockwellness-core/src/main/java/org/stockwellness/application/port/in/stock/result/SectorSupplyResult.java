package org.stockwellness.application.port.in.stock.result;

public record SectorSupplyResult(
    String sectorCode,
    String sectorName,
    Long netForeignBuyAmount,
    Long netInstBuyAmount,
    Integer foreignConsecutiveBuyDays,
    Integer instConsecutiveBuyDays
) {}
