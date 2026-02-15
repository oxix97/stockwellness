package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.LoadStockPricePort;
import org.stockwellness.domain.stock.StockPrice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements LoadStockPricePort, LoadBenchmarkPort {

    private final StockPriceRepository stockPriceRepository;
    private final BenchmarkRepository benchmarkRepository;
    private final StockPriceCacheAdapter stockPriceCacheAdapter;

    @Override
    public Optional<StockPrice> findLateststockPrice(String isinCode) {
        return Optional.empty();
    }

    @Override
    public List<StockPrice> loadRecentHistories(String isinCode, int limit) {
        return List.of();
    }

    @Override
    public Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit) {
        return Map.of();
    }

    @Override
    public List<StockPriceResult> loadPricesByYear(String ticker, int year) {
        return stockPriceCacheAdapter.loadPricesByYear(ticker, year);
    }

    @Override
    public List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end) {
        List<StockPriceResult> allPrices = new ArrayList<>();
        int startYear = start.getYear();
        int endYear = end.getYear();

        for (int year = startYear; year <= endYear; year++) {
            List<StockPriceResult> yearPrices = stockPriceCacheAdapter.loadPricesByYear(ticker, year);
            
            // 기간에 해당하는 데이터만 필터링
            final int currentYear = year;
            List<StockPriceResult> filtered = yearPrices.stream()
                    .filter(p -> !p.baseDate().isBefore(start) && !p.baseDate().isAfter(end))
                    .toList();
            allPrices.addAll(filtered);
        }

        return allPrices;
    }

    @Override
    public List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end) {
        return benchmarkRepository.findBenchmarkPrices(benchmarkTicker, start, end);
    }
}
