package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketIndexService implements MarketIndexUseCase {

    private final LoadBenchmarkPort loadBenchmarkPort;
    private final StockPricePort stockPricePort;
    private final MarketBreadthCalculator marketBreadthCalculator;
    private final MarketWeatherClassifier marketWeatherClassifier;

    @Override
    @Cacheable(value = "marketDashboard:v1", key = "'all'", sync = true)
    public MarketDashboardResult getMarketIndexes() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(MarketWeatherPolicy.LOOKBACK_DAYS);

        // 1. 모든 벤치마크 지수를 한 번에 조회 (9번 쿼리 -> 1번 쿼리로 통합)
        List<String> tickers = java.util.Arrays.stream(BenchmarkType.values())
                .map(BenchmarkType::getTicker)
                .toList();

        List<StockPriceResult> allPrices = loadBenchmarkPort.loadBenchmarkPricesIn(tickers, start, end);
        Map<String, List<StockPriceResult>> priceMap = allPrices.stream()
                .collect(java.util.stream.Collectors.groupingBy(StockPriceResult::ticker));

        List<MarketIndexResult> results = new ArrayList<>();
        List<String> missingTickers = new ArrayList<>();

        for (BenchmarkType type : BenchmarkType.values()) {
            List<StockPriceResult> prices = priceMap.get(type.getTicker());
            if (prices == null || prices.isEmpty()) {
                log.warn("[지수 서비스] 조회된 지수 데이터가 없습니다. ticker={}, name={}, range={}~{}",
                        type.getTicker(), type.getName(), start, end);
                missingTickers.add(type.getTicker());
                continue;
            }
            results.add(toResult(type, prices));
        }

        if (!missingTickers.isEmpty()) {
            log.error("[지수 서비스] 일부 또는 전체 시장 지수 데이터가 비어 있습니다. missingTickers={}, range={}~{}",
                    missingTickers, start, end);
            throw new StockPriceException(ErrorCode.PRICE_DATA_NOT_FOUND);
        }

        MarketWeatherResult weather = buildMarketWeather(results);
        return new MarketDashboardResult(results, weather);
    }

    private MarketIndexResult toResult(BenchmarkType type, List<StockPriceResult> prices) {
        StockPriceResult latest = prices.get(prices.size() - 1);
        BigDecimal currentPrice = latest.closePrice();
        BigDecimal fluctuationRate = latest.changeRate() != null ?
                latest.changeRate().setScale(MarketWeatherPolicy.DISPLAY_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal fluctuationAmount = BigDecimal.ZERO;
        if (fluctuationRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal rateMultiplier = fluctuationRate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(rateMultiplier);

            if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal prevPrice = currentPrice.divide(divisor, 4, RoundingMode.HALF_UP);
                fluctuationAmount = currentPrice.subtract(prevPrice);
            }
        }

        List<HistoryPoint> history = List.of(new HistoryPoint(latest.baseDate(), latest.closePrice()));

        return new MarketIndexResult(type.getTicker(), type.getName(), currentPrice, fluctuationRate, fluctuationAmount, history);
    }

    private MarketWeatherResult buildMarketWeather(List<MarketIndexResult> indexes) {
        MarketIndexResult kospi = findRequiredIndex(indexes, BenchmarkType.KOSPI);
        MarketIndexResult kosdaq = findRequiredIndex(indexes, BenchmarkType.KOSDAQ);

        LocalDate benchmarkDate = kospi.history().isEmpty() ? LocalDate.now() : kospi.history().getFirst().date();
        LocalDate breadthDate = stockPricePort.findLatestDateOnOrBefore(benchmarkDate).orElse(null);

        // 2. 전 종목 시세 데이터를 가져와서 앱 서버에서 계산하던 로직을 DB 집계로 전환
        MarketBreadthSnapshot breadth = (breadthDate == null)
                ? null
                : stockPricePort.summarizeBreadthByDate(breadthDate);

        LocalDate asOfDate = breadthDate != null ? breadthDate : benchmarkDate;
        return marketWeatherClassifier.classify(kospi.fluctuationRate(), kosdaq.fluctuationRate(), breadth, asOfDate);
    }

    private MarketIndexResult findRequiredIndex(List<MarketIndexResult> indexes, BenchmarkType type) {
        return indexes.stream()
                .filter(index -> type.getTicker().equals(index.ticker()))
                .findFirst()
                .orElseThrow(() -> new StockPriceException(ErrorCode.PRICE_DATA_NOT_FOUND));
    }
}
