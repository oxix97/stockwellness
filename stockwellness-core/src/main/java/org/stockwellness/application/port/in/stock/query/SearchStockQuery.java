package org.stockwellness.application.port.in.stock.query;

import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;

public record SearchStockQuery(
        String keyword,
        MarketType marketType,
        StockStatus status,
        int page,
        int size
) {}
