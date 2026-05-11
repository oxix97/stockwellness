package org.stockwellness.application.port.in.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

public interface BenchmarkPriceSyncUseCase {

    BenchmarkPriceResult toBenchmarkPrice(BenchmarkPriceCommand command);

    record BenchmarkPriceCommand(
            BenchmarkType type,
            LocalDate baseDate,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal changeRate,
            Long volume
    ) {
    }

    record BenchmarkPriceResult(BenchmarkPrice benchmarkPrice) {
    }
}
