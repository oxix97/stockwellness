package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.time.LocalDate;
import java.util.Optional;

public interface BenchmarkPricePort {
    Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate);
    void save(BenchmarkPrice benchmarkPrice);
}
