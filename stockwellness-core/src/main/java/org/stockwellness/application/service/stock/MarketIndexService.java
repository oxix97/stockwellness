package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    public MarketDashboardResult getMarketIndexes() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(MarketWeatherPolicy.LOOKBACK_DAYS);

        List<MarketIndexResult> results = new ArrayList<>();
        List<String> missingTickers = new ArrayList<>();

        for (BenchmarkType type : BenchmarkType.values()) {
            try {
                // BenchmarkType의 ticker()를 식별자로 사용하여 데이터 로드
                List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(type.getTicker(), start, end);
                if (prices.isEmpty()) {
                    log.warn("[지수 서비스] 조회된 지수 데이터가 없습니다. ticker={}, name={}, range={}~{}",
                            type.getTicker(), type.getName(), start, end);
                    missingTickers.add(type.getTicker());
                    continue;
                }
                results.add(toResult(type, prices));
            } catch (StockPriceException e) {
                log.warn("[지수 서비스] 시장 지수 조회 실패: {}({}) - {}", type.getName(), type.getTicker(), e.getMessage());
                missingTickers.add(type.getTicker());
            } catch (Exception e) {
                log.error("[지수 서비스] 시장 지수 조회 중 예기치 않은 오류 발생: {}({}) - {}",
                        type.getName(), type.getTicker(), e.getMessage());
                missingTickers.add(type.getTicker());
            }
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
        
        // fluctuationAmount = currentPrice - prevPrice
        // prevPrice = currentPrice / (1 + fluctuationRate/100)
        BigDecimal fluctuationAmount = BigDecimal.ZERO;
        if (fluctuationRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal rateMultiplier = fluctuationRate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(rateMultiplier);
            
            if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal prevPrice = currentPrice.divide(divisor, 4, RoundingMode.HALF_UP);
                fluctuationAmount = currentPrice.subtract(prevPrice);
            }
        }

        // 전체 날짜 대신 가장 최근 날짜의 시세만 배열에 담아 반환 (페이로드 최적화)
        List<HistoryPoint> history = List.of(new HistoryPoint(latest.baseDate(), latest.closePrice()));

        return new MarketIndexResult(type.getTicker(), type.getName(), currentPrice, fluctuationRate, fluctuationAmount, history);
    }

    private MarketWeatherResult buildMarketWeather(List<MarketIndexResult> indexes) {
        MarketIndexResult kospi = findRequiredIndex(indexes, BenchmarkType.KOSPI);
        MarketIndexResult kosdaq = findRequiredIndex(indexes, BenchmarkType.KOSDAQ);

        LocalDate benchmarkDate = kospi.history().isEmpty() ? LocalDate.now() : kospi.history().getFirst().date();
        LocalDate breadthDate = stockPricePort.findLatestDateOnOrBefore(benchmarkDate).orElse(null);
        MarketBreadthSnapshot breadth = breadthDate == null
                ? null
                : marketBreadthCalculator.summarize(stockPricePort.findAllByDate(breadthDate));

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
