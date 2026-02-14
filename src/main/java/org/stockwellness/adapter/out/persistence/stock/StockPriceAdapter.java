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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements LoadStockPricePort, LoadBenchmarkPort {

    private final StockPriceRepository stockPriceRepository;
    private final BenchmarkRepository benchmarkRepository;

    @Override
    public Optional<StockPrice> findLateststockPrice(String isinCode) {
        // 기존 메서드는 유지 (필요 시 구현)
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
        return stockPriceRepository.findAllByTickerAndYear(ticker, year);
    }

    @Override
    public List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end) {
        return stockPriceRepository.findAllByTickerAndPeriod(ticker, start, end);
    }

    @Override
    public List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end) {
        return benchmarkRepository.findBenchmarkPrices(benchmarkTicker, start, end);
    }
}
