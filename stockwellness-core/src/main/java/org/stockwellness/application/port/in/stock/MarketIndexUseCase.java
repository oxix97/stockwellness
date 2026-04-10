package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;

public interface MarketIndexUseCase {
    MarketDashboardResult getMarketIndexes();
}
