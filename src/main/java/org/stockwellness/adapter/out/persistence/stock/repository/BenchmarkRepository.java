package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.util.List;

public interface BenchmarkRepository {
    /**
     * 지수(Benchmark) 데이터를 조회합니다.
     */
    List<StockPriceResult> findBenchmarkPrices(String ticker, LocalDate start, LocalDate end);
}
