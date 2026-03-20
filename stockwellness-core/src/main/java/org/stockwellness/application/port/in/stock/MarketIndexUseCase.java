package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.MarketIndexResult;

import java.util.List;

public interface MarketIndexUseCase {
    List<MarketIndexResult> getMarketIndexes();
}
