package org.stockwellness.application.port.in.stock.result;

import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;

public record StockSearchResult(
        String ticker,
        String name,
        MarketType marketType,
        String sectorName,
        StockStatus status
) {
}
