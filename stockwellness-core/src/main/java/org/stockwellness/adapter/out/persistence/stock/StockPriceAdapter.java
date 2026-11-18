package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.MarketBreadthItem;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockInvestorTradeReadPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.domain.stock.price.TradeDirection;

@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements StockPricePort, LoadBenchmarkPort {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StockPriceReadPort stockPriceReadPort;
    private final StockInvestorTradeReadPort investorTradeReadPort;
    private final StockPort stockPort;
    private final IndexDailyReadPort indexDailyReadPort;

    @Override
    public List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(
            LocalDate date, TradeDirection direction, int limit) {
        return investorTradeReadPort.topStocks(date, "KRX", "INSTITUTION_TOTAL", direction, limit);
    }

    @Override
    public Optional<LocalDate> findLatestInvestorTradeDate() {
        return investorTradeReadPort.latestFinalDate("KRX");
    }

    @Override
    public List<StockSupplyRankingResult> findTopForeignStocksBySupply(
            LocalDate date, TradeDirection direction, int limit) {
        return investorTradeReadPort.topStocks(date, "KRX", "FOREIGN", direction, limit);
    }

    @Override
    public Map<Long, List<StockPrice>> findRecentPricesWithDateByStocks(List<Stock> stocks, LocalDate date, int limit) {
        if (stocks == null || stocks.isEmpty()) return Map.of();
        Map<Long, Stock> byId = stocks.stream().filter(StockPriceAdapter::isKoreanStock)
                .collect(Collectors.toMap(Stock::getId, stock -> stock));
        if (byId.isEmpty()) return Map.of();
        Map<Long, List<PriceObservation>> recent = stockPriceReadPort.recent(byId.keySet(), "KRX",
                date.minusDays(1), limit, PriceBasis.RAW);
        Map<Long, List<StockPrice>> results = new LinkedHashMap<>();
        recent.forEach((id, rows) -> {
            List<StockPrice> prices = rows.stream().map(row -> toStockPrice(byId.get(id), row))
                    .collect(Collectors.toCollection(ArrayList::new));
            java.util.Collections.reverse(prices);
            results.put(id, prices);
        });
        return results;
    }

    @Override
    public List<StockPrice> findRecentPricesWithDateByStock(Stock stock, LocalDate date, int limit) {
        if (!isKoreanStock(stock)) return List.of();
        return stockPriceReadPort.recent(Set.of(stock.getId()), "KRX", date, limit, PriceBasis.RAW)
                .getOrDefault(stock.getId(), List.of()).stream().map(row -> toStockPrice(stock, row)).toList();
    }

    @Override
    public Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date) {
        return stockPriceReadPort.latestFinalDate("KRX", date);
    }

    @Override
    public boolean existsByBaseDate(LocalDate date) {
        return stockPriceReadPort.latestFinalDate("KRX", date).filter(date::equals).isPresent();
    }

    @Override
    public Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) return Map.of();
        Set<Long> koreanStockIds = stocks.stream().filter(StockPriceAdapter::isKoreanStock)
                .map(Stock::getId).collect(Collectors.toSet());
        if (koreanStockIds.isEmpty()) return Map.of();
        Map<Long, PriceObservation> latest = stockPriceReadPort.latest(koreanStockIds, "KRX",
                LocalDate.now(SEOUL), PriceBasis.RAW);
        return latest.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().tradeDate()));
    }

    @Override
    public List<StockPrice> findAllByDate(LocalDate date) {
        List<Stock> stocks = activeKoreanStocks();
        if (stocks.isEmpty()) return List.of();
        Map<Long, Stock> byId = stocks.stream().collect(Collectors.toMap(Stock::getId, stock -> stock));
        return stockPriceReadPort.history(byId.keySet(), "KRX", date, date, PriceBasis.RAW).values().stream()
                .flatMap(List::stream).map(row -> toStockPrice(byId.get(row.stockId()), row)).toList();
    }

    @Override
    public List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate date) {
        List<Stock> stocks = activeKoreanStocks();
        if (stocks.isEmpty()) return List.of();
        return stockPriceReadPort.history(stocks.stream().map(Stock::getId).collect(Collectors.toSet()),
                        "KRX", date, date, PriceBasis.RAW).values().stream().flatMap(List::stream)
                .map(row -> new MarketBreadthItem(row.tradeDate(), row.openPrice(), row.highPrice(), row.lowPrice(),
                        row.closePrice(), row.previousClosePrice()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "marketBreadth:v2", key = "#date.toString()", unless = "#result == null")
    public MarketBreadthSnapshot summarizeBreadthByDate(LocalDate date) {
        List<MarketBreadthItem> items = findAllBreadthItemsByDate(date).stream()
                .filter(item -> positive(item.previousClosePrice()) && positive(item.closePrice())
                        && positive(item.highPrice()) && positive(item.lowPrice()))
                .toList();
        if (items.isEmpty()) return null;

        int advancing = 0;
        int declining = 0;
        int unchanged = 0;
        int highVolatility = 0;
        for (MarketBreadthItem item : items) {
            BigDecimal base = item.previousClosePrice();
            BigDecimal rate = item.closePrice().subtract(base).divide(base, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            BigDecimal swing = item.highPrice().subtract(item.lowPrice()).divide(base, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (rate.compareTo(new BigDecimal("0.15")) > 0) advancing++;
            else if (rate.compareTo(new BigDecimal("-0.15")) < 0) declining++;
            else unchanged++;
            if (rate.abs().compareTo(new BigDecimal("3.00")) >= 0
                    || swing.compareTo(new BigDecimal("4.00")) >= 0) highVolatility++;
        }

        BigDecimal total = BigDecimal.valueOf(items.size());
        return new MarketBreadthSnapshot(items.size(), advancing, declining, unchanged, highVolatility,
                ratio(advancing, total), ratio(declining, total), ratio(highVolatility, total));
    }

    @Override
    public Optional<StockPrice> findLateststockPrice(String isinCode) {
        return Optional.empty();
    }

    @Override
    public Optional<StockPrice> findLatestByTicker(String ticker) {
        return stockPort.loadStockByTicker(ticker).filter(StockPriceAdapter::isKoreanStock).flatMap(stock -> stockPriceReadPort
                .latest(Set.of(stock.getId()), "KRX", LocalDate.now(SEOUL), PriceBasis.RAW)
                .values().stream().findFirst().map(row -> toStockPrice(stock, row)));
    }

    @Override
    public Map<String, BigDecimal> findAllLatestByTickers(List<String> tickers) {
        Map<String, Stock> stocks = loadStocks(tickers);
        Map<Long, PriceObservation> latest = latestPrices(stocks.values());
        return stocks.entrySet().stream().filter(entry -> latest.containsKey(entry.getValue().getId()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> latest.get(entry.getValue().getId()).closePrice()));
    }

    @Override
    public Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> tickers, int limit) {
        if (tickers == null || tickers.isEmpty()) return Map.of();
        Map<String, Stock> stocks = loadStocks(tickers);
        if (stocks.isEmpty()) return Map.of();
        Map<Long, List<PriceObservation>> rows = stockPriceReadPort.recent(stocks.values().stream()
                        .map(Stock::getId).collect(Collectors.toSet()), "KRX", LocalDate.now(SEOUL), limit, PriceBasis.RAW);
        Map<String, List<StockPrice>> result = new LinkedHashMap<>();
        stocks.forEach((ticker, stock) -> {
            List<PriceObservation> observations = rows.getOrDefault(stock.getId(), List.of());
            if (!observations.isEmpty()) result.put(ticker,
                    observations.stream().map(row -> toStockPrice(stock, row)).toList());
        });
        return result;
    }

    @Override
    public List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end) {
        Stock stock = stockPort.loadStockByTicker(ticker).filter(StockPriceAdapter::isKoreanStock).orElse(null);
        if (stock == null) return List.of();
        return stockPriceReadPort.history(Set.of(stock.getId()), "KRX", start, end, PriceBasis.RAW)
                .getOrDefault(stock.getId(), List.of()).stream().map(row -> toResult(ticker, row)).toList();
    }

    @Override
    public Map<String, List<StockPriceResult>> loadPricesByTickers(List<String> tickers, LocalDate start, LocalDate end) {
        if (tickers == null || tickers.isEmpty()) return Map.of();
        Map<String, Stock> stocks = loadStocks(tickers);
        if (stocks.isEmpty()) return Map.of();
        Map<Long, List<PriceObservation>> rows = stockPriceReadPort.history(stocks.values().stream()
                        .map(Stock::getId).collect(Collectors.toSet()), "KRX", start, end, PriceBasis.RAW);
        Map<String, List<StockPriceResult>> result = new LinkedHashMap<>();
        stocks.forEach((ticker, stock) -> {
            List<PriceObservation> observations = rows.getOrDefault(stock.getId(), List.of());
            if (!observations.isEmpty()) result.put(ticker,
                    observations.stream().map(row -> toResult(ticker, row)).toList());
        });
        return result;
    }

    @Override
    public List<StockPriceResult> loadBenchmarkPrices(String benchmarkTicker, LocalDate start, LocalDate end) {
        String publicCode = canonicalBenchmarkCode(benchmarkTicker);
        if (publicCode == null) return List.of();
        return indexDailyReadPort.historyFinal(publicCode, start, end).stream()
                .map(observation -> toBenchmarkResult(benchmarkTicker, observation)).toList();
    }

    @Override
    public List<StockPriceResult> loadBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end) {
        if (tickers == null || tickers.isEmpty()) return List.of();
        return tickers.stream().flatMap(ticker -> loadBenchmarkPrices(ticker, start, end).stream())
                .sorted(Comparator.comparing(StockPriceResult::baseDate).thenComparing(StockPriceResult::ticker))
                .toList();
    }

    @Override
    public Optional<StockPriceResult> loadBenchmarkPriceAtOrBefore(String benchmarkTicker, LocalDate asOf) {
        String publicCode = canonicalBenchmarkCode(benchmarkTicker);
        if (publicCode == null) return Optional.empty();
        return indexDailyReadPort.latestFinal(publicCode, asOf)
                .map(observation -> toBenchmarkResult(benchmarkTicker, observation));
    }

    private List<Stock> activeKoreanStocks() {
        return stockPort.findAllByActiveStocks().stream().filter(StockPriceAdapter::isKoreanStock).toList();
    }

    private Map<String, Stock> loadStocks(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return Map.of();
        return stockPort.loadStocksByTickers(tickers).stream()
                .filter(StockPriceAdapter::isKoreanStock)
                .collect(Collectors.toMap(Stock::getTicker, stock -> stock, (first, ignored) -> first));
    }

    private Map<Long, PriceObservation> latestPrices(java.util.Collection<Stock> stocks) {
        if (stocks.isEmpty()) return Map.of();
        return stockPriceReadPort.latest(stocks.stream().map(Stock::getId).collect(Collectors.toSet()),
                "KRX", LocalDate.now(SEOUL), PriceBasis.RAW);
    }

    private static StockPrice toStockPrice(Stock stock, PriceObservation row) {
        TechnicalIndicators indicators = new TechnicalIndicators(row.ma5(), row.ma20(), row.ma60(), row.ma120(),
                row.rsi14(), row.macd(), row.macdSignal(), null, null, null, row.adx(), row.plusDi(), row.minusDi(),
                null, null, null, null, null);
        return StockPrice.of(stock, row.tradeDate(), row.openPrice(), row.highPrice(), row.lowPrice(), row.closePrice(),
                row.adjustedClosePrice(), row.previousClosePrice(), row.volume(), row.tradingAmount(), indicators);
    }

    private static StockPriceResult toResult(String ticker, PriceObservation row) {
        BigDecimal changeRate = rate(row.closePrice(), row.previousClosePrice());
        return new StockPriceResult(row.tradeDate(), row.openPrice(), row.highPrice(), row.lowPrice(), row.closePrice(),
                row.adjustedClosePrice(), row.volume(), row.tradingAmount(), row.ma5(), row.ma20(), row.ma60(),
                row.ma120(), changeRate, ticker);
    }

    private static BigDecimal rate(BigDecimal close, BigDecimal previousClose) {
        if (!positive(close) || !positive(previousClose)) return null;
        return close.subtract(previousClose).divide(previousClose, new MathContext(16)).multiply(BigDecimal.valueOf(100));
    }

    private static BigDecimal ratio(int count, BigDecimal total) {
        return BigDecimal.valueOf(count).divide(total, 4, RoundingMode.HALF_UP);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean isKoreanStock(Stock stock) {
        return stock != null && (stock.getMarketType() == org.stockwellness.domain.stock.MarketType.KOSPI
                || stock.getMarketType() == org.stockwellness.domain.stock.MarketType.KOSDAQ);
    }

    private static String canonicalBenchmarkCode(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        String normalized = ticker.toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "^KS11" -> "0001";
            case "^KQ11" -> "1001";
            case "^GSPC" -> "SPX";
            case "0001", "1001", "2001", "3003", "SPX", "NDX", ".DJI" -> normalized;
            default -> null;
        };
    }

    private static StockPriceResult toBenchmarkResult(String ticker, IndexPriceObservation observation) {
        BigDecimal close = observation.closePrice();
        BigDecimal previousClose = observation.previousClosePrice();
        BigDecimal changeRate = rate(close, previousClose);
        return new StockPriceResult(observation.tradeDate(), observation.openPrice(), observation.highPrice(),
                observation.lowPrice(), close, close, observation.volume(), observation.tradingAmount(),
                null, null, null, null, changeRate, ticker);
    }
}
