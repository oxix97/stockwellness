package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketIndexService implements MarketIndexUseCase {

    private final LoadBenchmarkPort loadBenchmarkPort;

    private static final int HISTORY_DAYS = 30;
    private static final int DISPLAY_SCALE = 2;

    private record IndexDef(String name, String ticker) {}

    private static final List<IndexDef> INDEXES = List.of(
            new IndexDef("KOSPI", "^KS11"),
            new IndexDef("KOSDAQ", "^KQ11"),
            new IndexDef("S&P500", "^GSPC")
    );

    @Override
    public List<MarketIndexResult> getMarketIndexes() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(HISTORY_DAYS);

        List<MarketIndexResult> results = new ArrayList<>();
        for (IndexDef index : INDEXES) {
            try {
                List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(index.ticker(), start, end);
                results.add(toResult(index.name(), prices));
            } catch (Exception e) {
                log.warn("시장 지수 조회 실패: {}", index.name(), e);
                results.add(emptyResult(index.name()));
            }
        }
        return results;
    }

    private MarketIndexResult toResult(String name, List<StockPriceResult> prices) {
        if (prices.isEmpty()) {
            return emptyResult(name);
        }

        StockPriceResult latest = prices.get(prices.size() - 1);
        BigDecimal currentPrice = latest.closePrice();
        BigDecimal fluctuationRate = BigDecimal.ZERO;

        if (prices.size() >= 2) {
            StockPriceResult prev = prices.get(prices.size() - 2);
            BigDecimal prevClose = prev.closePrice();
            if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                fluctuationRate = currentPrice.subtract(prevClose)
                        .divide(prevClose, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
            }
        }

        List<HistoryPoint> history = prices.stream()
                .map(p -> new HistoryPoint(p.baseDate(), p.closePrice()))
                .toList();

        return new MarketIndexResult(name, currentPrice, fluctuationRate, history);
    }

    private MarketIndexResult emptyResult(String name) {
        return new MarketIndexResult(name, BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList());
    }
}
