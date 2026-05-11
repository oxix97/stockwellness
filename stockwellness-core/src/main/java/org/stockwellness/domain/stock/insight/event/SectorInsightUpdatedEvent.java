package org.stockwellness.domain.stock.insight.event;

import java.time.LocalDate;

import org.stockwellness.domain.stock.MarketType;

public record SectorInsightUpdatedEvent(
    MarketType marketType,
    LocalDate baseDate,
    String status
) {}
