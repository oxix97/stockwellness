package org.stockwellness.adapter.out.persistence.stock.repository;

import java.time.LocalDate;
import java.util.List;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

public interface BenchmarkRepository {
    /**
     * 지수(Benchmark) 데이터를 조회합니다.
     */
    List<StockPriceResult> findBenchmarkPrices(String ticker, LocalDate start, LocalDate end);

    /**
     * 여러 지수(Benchmark) 데이터를 한 번에 조회합니다.
     */
    List<StockPriceResult> findBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end);
}
