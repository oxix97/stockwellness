package org.stockwellness.domain.stock.insight.event;

import org.stockwellness.domain.stock.MarketType;
import java.time.LocalDate;

public record SectorInsightUpdatedEvent(
    MarketType marketType,
    LocalDate baseDate,
    String status
) {}
