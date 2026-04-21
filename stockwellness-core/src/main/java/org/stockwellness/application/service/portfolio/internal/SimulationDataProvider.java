package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPricePort;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.stock.BenchmarkType;

@Component
@RequiredArgsConstructor
public class SimulationDataProvider {

    private final StockPricePort stockPricePort;
    private final LoadBenchmarkPort loadBenchmarkPort;

    public SimulationData loadData(List<String> symbols, List<String> benchmarkTickers, LocalDate start, LocalDate end) {
        Map<String, List<StockPriceResult>> stockPrices = stockPricePort.loadPricesByTickers(symbols, start, end);
        
        Map<String, List<StockPriceResult>> benchmarkPrices = new HashMap<>();
        for (String ticker : benchmarkTickers) {
            List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(ticker, start, end);
            if (!prices.isEmpty()) {
                benchmarkPrices.put(ticker, prices);
            }
        }

        return new SimulationData(stockPrices, benchmarkPrices);
    }
}
