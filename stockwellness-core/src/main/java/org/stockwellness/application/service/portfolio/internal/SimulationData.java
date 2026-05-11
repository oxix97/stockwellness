package org.stockwellness.application.service.portfolio.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

public record SimulationData(
    Map<String, List<StockPriceResult>> stockPrices,
    Map<String, List<StockPriceResult>> benchmarkPrices // Ticker -> Prices (KOSPI, KOSDAQ, etc.)
) {
    public List<StockPriceResult> getBenchmarkPrices(String ticker) {
        return benchmarkPrices.getOrDefault(ticker, Collections.emptyList());
    }
}
