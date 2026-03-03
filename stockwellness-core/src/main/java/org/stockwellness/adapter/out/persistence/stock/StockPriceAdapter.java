package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements StockPricePort, LoadBenchmarkPort {

    private final StockPriceRepository stockPriceRepository;
    private final BenchmarkRepository benchmarkRepository;
    private final StockPriceCacheAdapter stockPriceCacheAdapter;
    private final KisDailyPriceAdapter kisAdapter;

    @Override
    public List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers) {
        return kisAdapter.fetchMultiStockPrices(tickers);
    }

    @Override
    public List<Stock> fetchDaily(LocalDate date) {
        return List.of();
    }

    @Override
    public List<StockPrice> fetchDailyPrice(LocalDate date) {
        return List.of();
    }

    @Override
    public List<BigDecimal> findRecentClosingPrices(Stock stock, LocalDate date, int limit) {
        List<BigDecimal> prices = stockPriceRepository.findRecentClosingPrices(stock, date, PageRequest.of(0, limit));
        Collections.reverse(prices);
        return prices;
    }

    @Override
    public Map<Long, List<BigDecimal>> findRecentClosingPricesByStocks(List<Stock> stocks, LocalDate date, int limit) {
        // QueryDSL로 최적화된 벌크 조회 호출
        List<StockPrice> allPrices = stockPriceRepository.findRecentPricesByStocks(stocks, date);
        
        return allPrices.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStock().getId(),
                        Collectors.mapping(StockPrice::getClosePrice, Collectors.toList())
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<BigDecimal> prices = e.getValue().stream().limit(limit).collect(Collectors.toList());
                            Collections.reverse(prices);
                            return prices;
                        }
                ));
    }

    @Override
    public LocalDate findLatestBaseDate(Stock stock) {
        return stockPriceRepository.findLatestBaseDate(stock);
    }

    @Override
    public Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks) {
        // QueryDSL로 직접 Map 반환받음
        return stockPriceRepository.findLatestBaseDatesByStocks(stocks);
    }

    @Override
    public List<StockPrice> findByStocksAndDate(List<Stock> stocks, LocalDate date) {
        return stockPriceRepository.findByStockInAndIdBaseDate(stocks, date);
    }

    @Override
    public List<StockPrice> findAllByDate(LocalDate date) {
        return stockPriceRepository.findAllByIdBaseDate(date);
    }

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
