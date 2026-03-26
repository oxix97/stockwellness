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

    public SimulationData loadData(List<String> symbols, String benchmarkTicker, LocalDate start, LocalDate end) {
        Map<String, List<StockPriceResult>> stockPrices = stockPricePort.loadPricesByTickers(symbols, start, end);
        
        // 주요 지수(KOSPI, KOSDAQ, KOSPI_200 등) 모두 로드
        Map<String, List<StockPriceResult>> benchmarkPrices = new HashMap<>();
        for (BenchmarkType type : BenchmarkType.values()) {
            List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(type.getTicker(), start, end);
            if (!prices.isEmpty()) {
                benchmarkPrices.put(type.getTicker(), prices);
            }
        }

        // 요청된 benchmarkTicker가 로드되지 않았을 경우 추가 로드 시도
        if (benchmarkTicker != null && !benchmarkPrices.containsKey(benchmarkTicker)) {
            List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(benchmarkTicker, start, end);
            if (!prices.isEmpty()) {
                benchmarkPrices.put(benchmarkTicker, prices);
            }
        }

        return new SimulationData(stockPrices, benchmarkPrices);
    }
}
