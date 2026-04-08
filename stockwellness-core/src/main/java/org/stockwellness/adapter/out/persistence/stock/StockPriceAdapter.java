package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.MultiStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.AlignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements StockPricePort, LoadBenchmarkPort, BenchmarkPricePort {

    private final StockPriceRepository stockPriceRepository;
    private final BenchmarkRepository benchmarkRepository;
    private final BenchmarkPriceRepository benchmarkPriceRepository;
    private final StockPriceCacheAdapter stockPriceCacheAdapter;
    private final KisDailyPriceAdapter kisAdapter;

    @Override
    public Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate) {
        return benchmarkPriceRepository.findByTickerAndBaseDate(ticker, baseDate);
    }

    @Override
    public Optional<BenchmarkPrice> findLatestBefore(String ticker, LocalDate baseDate) {
        return benchmarkPriceRepository.findTopByTickerAndBaseDateLessThanOrderByBaseDateDesc(ticker, baseDate);
    }

    @Override
    public void save(BenchmarkPrice benchmarkPrice) {
        benchmarkPriceRepository.save(benchmarkPrice);
    }

    @Override
    public List<StockPrice> findFilteredStocksByIndicators(
            LocalDate baseDate,
            AlignmentStatus alignment,
            BigDecimal rsiLow,
            BigDecimal rsiHigh,
            Boolean isGoldenCross
    ) {
        return stockPriceRepository.findFilteredStocksByIndicators(baseDate, alignment, rsiLow, rsiHigh, isGoldenCross);
    }

    @Override
    public List<MultiStockPriceSnapshot> fetchMultiStockPrices(List<String> tickers) {
        return kisAdapter.fetchMultiStockPrices(tickers).stream()
                .map(detail -> new MultiStockPriceSnapshot(
                        detail.ticker(),
                        detail.name(),
                        toBigDecimal(detail.closePrice()),
                        toBigDecimal(detail.priceChange()),
                        toBigDecimal(detail.priceChangeRate()),
                        toBigDecimal(detail.openPrice()),
                        toBigDecimal(detail.highPrice()),
                        toBigDecimal(detail.lowPrice()),
                        toLong(detail.accumulatedVolume()),
                        toBigDecimal(detail.accumulatedTradingValue()),
                        toBigDecimal(detail.previousClosePrice()),
                        toBigDecimal(detail.netInstitutionalBuyingAmt()),
                        toBigDecimal(detail.netForeignBuyingAmt())
                ))
                .toList();
    }

    @Override
    public List<DailyStockPriceSnapshot> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        return kisAdapter.fetchDailyPrices(stock, startDate, endDate).stream()
                .map(detail -> new DailyStockPriceSnapshot(
                        detail.baseDate(),
                        detail.openPrice(),
                        detail.highPrice(),
                        detail.lowPrice(),
                        detail.closePrice(),
                        detail.volume(),
                        detail.transactionAmt()
                ))
                .toList();
    }

    @Override
    public List<InvestorTradingSnapshot> fetchInvestorTradingSnapshots(Stock stock, LocalDate startDate, LocalDate endDate) {
        return kisAdapter.fetchInvestorPrices(stock, startDate, endDate).stream()
                .map(detail -> new InvestorTradingSnapshot(
                        detail.baseDate(),
                        detail.closePrice(),
                        detail.prdyCtrt(),
                        detail.volume(),
                        detail.netInstitutionalBuyingQty(),
                        detail.netForeignBuyingQty(),
                        detail.netInstitutionalBuyingAmt(),
                        detail.netForeignBuyingAmt()
                ))
                .toList();
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
        // [수정] limit 파라미터 전달 보강
        List<StockPrice> allPrices = stockPriceRepository.findRecentPricesByStocks(stocks, date, limit);

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
    public Map<Long, List<StockPrice>> findRecentPricesWithDateByStocks(List<Stock> stocks, LocalDate date, int limit) {
        // [수정] limit 파라미터 전달 보강
        List<StockPrice> allPrices = stockPriceRepository.findRecentPricesByStocks(stocks, date, limit);

        return allPrices.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStock().getId()
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<StockPrice> prices = e.getValue().stream().limit(limit).collect(Collectors.toList());
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
    public Optional<StockPrice> findLatestByTicker(String ticker) {
        return stockPriceRepository.findTopByStockTickerOrderByIdBaseDateDesc(ticker);
    }

    @Override
    public Map<String, BigDecimal> findAllLatestByTickers(List<String> tickers) {
        // [N+1 해결] 여러 티커의 최신 종가를 한 번의 QueryDSL 쿼리로 조회
        return stockPriceRepository.findLatestPricesByTickers(tickers);
    }

    @Override
    public List<StockPrice> loadRecentHistories(String isinCode, int limit) {
        if (isinCode == null || isinCode.isBlank()) return List.of();
        return stockPriceRepository.findRecentPrices(isinCode, LocalDate.now(), PageRequest.of(0, limit));
    }

    @Override
    public Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit) {
        if (isinCodes == null || isinCodes.isEmpty()) return Map.of();
        Map<String, List<StockPrice>> result = new HashMap<>();
        for (String ticker : isinCodes) {
            List<StockPrice> prices = stockPriceRepository.findRecentPrices(ticker, LocalDate.now(), PageRequest.of(0, limit));
            if (prices != null && !prices.isEmpty()) {
                result.put(ticker, prices);
            }
        }
        return result;
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
    public Map<String, List<StockPriceResult>> loadPricesByTickers(List<String> tickers, LocalDate start, LocalDate end) {
        return stockPriceRepository.findByStockTickerInAndIdBaseDateBetween(tickers, start, end).stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStock().getTicker(),
                        Collectors.mapping(p -> {
                            var indicators = p.getIndicators();
                            return new StockPriceResult(
                                    p.getId().getBaseDate(),
                                    p.getOpenPrice(),
                                    p.getHighPrice(),
                                    p.getLowPrice(),
                                    p.getClosePrice(),
                                    p.getAdjClosePrice(),
                                    p.getVolume(),
                                    p.getTransactionAmt(),
                                    indicators != null ? indicators.getMa5() : null,
                                    indicators != null ? indicators.getMa20() : null,
                                    indicators != null ? indicators.getMa60() : null,
                                    indicators != null ? indicators.getMa120() : null,
                                    p.getFluctuationRate()
                            );
                        }, Collectors.toList())
                ));
    }

    @Override
    public List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end) {
        return benchmarkRepository.findBenchmarkPrices(benchmarkTicker, start, end);
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }
}
