package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.MarketBreadthItem;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements StockPricePort, LoadBenchmarkPort, BenchmarkPricePort {

    private final StockPriceRepository stockPriceRepository;
    private final BenchmarkRepository benchmarkRepository;
    private final BenchmarkPriceRepository benchmarkPriceRepository;
    private final StockPriceCacheAdapter stockPriceCacheAdapter;

    @Override
    public StockPrice save(StockPrice stockPrice) {
        return stockPriceRepository.save(stockPrice);
    }

    public List<StockPrice> findRecent120Prices(Long stockId) {
        return stockPriceRepository.findRecent120Prices(stockId, PageRequest.of(0, 120));
    }

    @Override
    public Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate) {
        return benchmarkPriceRepository.findByTickerAndBaseDate(ticker, baseDate);
    }

    @Override
    public Optional<BenchmarkPrice> findLatestBefore(String ticker, LocalDate baseDate) {
        return benchmarkPriceRepository.findTopByTickerAndBaseDateLessThanOrderByBaseDateDesc(ticker, baseDate);
    }

    @Override
    public List<BenchmarkPrice> findHistoryByTicker(String ticker, LocalDate endDate, int limit) {
        return benchmarkPriceRepository.findByTickerAndBaseDateLessThanEqualOrderByBaseDateDesc(ticker, endDate, PageRequest.of(0, limit));
    }

    @Override
    public void save(BenchmarkPrice benchmarkPrice) {
        benchmarkPriceRepository.save(benchmarkPrice);
    }

    @Override
    public List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    ) {
        return stockPriceRepository.findTopInstitutionStocksBySupply(date, direction, limit);
    }

    @Override
    public Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date) {
        return stockPriceRepository.findLatestDateOnOrBefore(date);
    }

    @Override
    public Optional<LocalDate> findLatestInvestorTradeDate() {
        return stockPriceRepository.findLatestInvestorTradeDate();
    }

    @Override
    public boolean existsByBaseDate(LocalDate date) {
        return stockPriceRepository.countByBaseDate(date) > 0;
    }

    @Override
    public void saveAll(List<StockPrice> stockPrices) {
        stockPriceRepository.saveAll(stockPrices);
    }

    @Override
    public List<StockSupplyRankingResult> findTopForeignStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    ) {
        return stockPriceRepository.findTopForeignStocksBySupply(date, direction, limit);
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
    public List<StockPrice> findRecentPricesWithDateByStock(Stock stock, LocalDate date, int limit) {
        return stockPriceRepository.findRecentPricesByStock(stock, date, PageRequest.of(0, limit));
    }

    @Override
    public Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks) {
        // QueryDSL로 직접 Map 반환받음
        return stockPriceRepository.findLatestBaseDatesByStocks(stocks);
    }

    @Override
    public List<StockPrice> findAllByDate(LocalDate date) {
        return stockPriceRepository.findAllByDateWithStock(date);
    }

    @Override
    public List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate date) {
        return stockPriceRepository.findAllBreadthItemsByDate(date);
    }

    @Override
    @Cacheable(cacheNames = "marketBreadth:v1", key = "#date.toString()", unless = "#result == null")
    public MarketBreadthSnapshot summarizeBreadthByDate(LocalDate date) {
        return stockPriceRepository.summarizeBreadthByDate(date);
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
    public Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit) {
        if (isinCodes == null || isinCodes.isEmpty()) return Map.of();
        // [N+1 해결] 루프를 제거하고 Repository에서 일괄 조회하도록 변경
        return stockPriceRepository.findRecentPricesBatch(isinCodes, limit);
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

    @Override
    public List<StockPriceResult> loadBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end) {
        return benchmarkRepository.findBenchmarkPricesIn(tickers, start, end);
    }
}
