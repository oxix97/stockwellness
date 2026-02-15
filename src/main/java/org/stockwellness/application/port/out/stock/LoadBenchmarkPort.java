package org.stockwellness.application.port.out.stock;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.util.List;

public interface LoadBenchmarkPort {
    /**
     * 벤치마크 지수의 기간별 데이터를 로드합니다.
     */
    List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end);
}
