package org.stockwellness.application.service.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BenchmarkPriceSyncService implements BenchmarkPriceSyncUseCase {

    private final BenchmarkPricePort benchmarkPricePort;
    private final Map<String, BigDecimal> prevCloseMap = new HashMap<>();

    @Override
    public BenchmarkPriceResult toBenchmarkPrice(BenchmarkPriceCommand command) {
        String ticker = command.type().getTicker();
        BigDecimal close = command.closePrice();
        BigDecimal changeRate = command.changeRate();
        BigDecimal prevClose = prevCloseMap.get(ticker);

        if (command.type().isOverseas() || changeRate == null || changeRate.compareTo(BigDecimal.ZERO) == 0) {
            if (prevClose == null) {
                prevClose = benchmarkPricePort.findLatestBefore(ticker, command.baseDate())
                        .map(BenchmarkPrice::getClosePrice)
                        .orElse(null);
            }
            if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                changeRate = close.subtract(prevClose)
                        .divide(prevClose, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                changeRate = BigDecimal.ZERO;
            }
        }

        BenchmarkPrice benchmarkPrice = BenchmarkPrice.of(
                command.type().getName(),
                ticker,
                command.baseDate(),
                close
        );
        benchmarkPrice.updatePrices(
                command.openPrice(),
                command.highPrice(),
                command.lowPrice(),
                close,
                changeRate,
                command.volume()
        );
        prevCloseMap.put(ticker, close);
        return new BenchmarkPriceResult(benchmarkPrice);
    }
}
