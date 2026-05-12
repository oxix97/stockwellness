package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

public interface LoadBenchmarkPort {
    /**
     * 지수(Benchmark) 데이터를 조회합니다.
     */
    List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end);

    /**
     * 여러 지수(Benchmark) 데이터를 한 번에 조회합니다.
     */
    List<StockPriceResult> loadBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end);
}

