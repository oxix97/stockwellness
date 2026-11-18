package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.price.PriceBasis;

@Component
@RequiredArgsConstructor
public class StockDataAdapter implements StockDataPort {

    private final StockPriceReadPort stockPriceReadPort;

    @Override
    public Map<Long, StockWellnessDetail> getStockDetails(List<Stock> stocks) {
        Map<Long, Stock> stocksById = stocks.stream().collect(Collectors.toMap(Stock::getId, stock -> stock));
        Set<Long> stockIds = stocksById.values().stream()
                .filter(stock -> stock.getMarketType() == MarketType.KOSPI || stock.getMarketType() == MarketType.KOSDAQ)
                .map(Stock::getId)
                .collect(Collectors.toSet());
        if (stockIds.isEmpty()) return Map.of();

        Map<Long, List<PriceObservation>> recent = stockPriceReadPort.recent(
                stockIds, "KRX", LocalDate.now(), 2, PriceBasis.RAW);
        return stocksById.entrySet().stream()
                .filter(entry -> recent.containsKey(entry.getKey()) && !recent.get(entry.getKey()).isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    List<PriceObservation> history = recent.get(entry.getKey());
                    PriceObservation latest = history.getFirst();
                    PriceObservation previous = history.size() > 1 ? history.get(1) : null;
                    BigDecimal fluctuationRate = previous == null || previous.closePrice().signum() == 0 ? null
                            : latest.closePrice().subtract(previous.closePrice())
                                    .divide(previous.closePrice(), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                    return new StockWellnessDetail(entry.getValue().getStandardCode(), latest.closePrice(), fluctuationRate, null,
                            TechnicalCalculator.analyzeRsiLevel(null),
                            "데이터가 부족하여 AI 분석을 제공할 수 없습니다.");
                }));
    }
}
