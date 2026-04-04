package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.time.LocalDate;
import java.util.Optional;

public interface BenchmarkPricePort {
    Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate);

    /**
     * 특정 지수의 특정 일자 이전 가장 최근 시세를 조회합니다.
     */
    Optional<BenchmarkPrice> findLatestBefore(String ticker, LocalDate baseDate);

    void save(BenchmarkPrice benchmarkPrice);
}
