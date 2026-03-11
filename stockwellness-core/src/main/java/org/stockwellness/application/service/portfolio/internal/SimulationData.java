package org.stockwellness.application.service.portfolio.internal;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.util.List;
import java.util.Map;

public record SimulationData(
    Map<String, List<StockPriceResult>> stockPrices,
    List<StockPriceResult> benchmarkPrices
) {}
