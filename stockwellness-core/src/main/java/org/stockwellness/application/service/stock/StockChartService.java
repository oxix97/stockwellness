package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.StockPriceUseCase;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse.BenchmarkPoint;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse.ChartPoint;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.application.port.out.stock.LoadStockPricePort;
import org.stockwellness.domain.stock.price.ChartPeriod;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockChartService implements StockPriceUseCase {

    private final LoadStockPricePort loadStockPricePort;
    private final LoadBenchmarkPort loadBenchmarkPort;
    private final LoadStockPort loadStockPort;

    private static final String DEFAULT_BENCHMARK = "^KS11"; // KOSPI
    private static final int CALC_SCALE = 8;
    private static final int DISPLAY_SCALE = 4;

    @Override
    public ChartDataResponse loadChartData(ChartQuery query) {
        validateStock(query.ticker());

        LocalDate end = LocalDate.now();
        LocalDate start = query.period().calculateStartDate(end);

        List<StockPriceResult> dailyPrices = loadStockPricePort.loadPricesByTicker(query.ticker(), start, end);
        if (dailyPrices.isEmpty()) {
            throw new StockPriceException(ErrorCode.PRICE_DATA_NOT_FOUND);
        }

        List<ChartPoint> aggregatedPrices = switch (query.frequency()) {
            case WEEKLY -> PriceDataAggregator.aggregateToWeekly(dailyPrices);
            case MONTHLY -> PriceDataAggregator.aggregateToMonthly(dailyPrices);
            case DAILY -> dailyPrices.stream().map(this::toChartPoint).toList();
        };

        List<BenchmarkPoint> benchmarks = Collections.emptyList();
        if (query.includeBenchmark()) {
            String benchmarkTicker = resolveBenchmarkTicker(query.ticker());
            try {
                List<StockPriceResult> benchmarkDaily = loadBenchmarkPort.loadBenchmarkPrices(benchmarkTicker, start, end);
                benchmarks = calculateBenchmarkReturns(benchmarkDaily);
            } catch (Exception e) {
                log.warn("Failed to load benchmark data for {}", benchmarkTicker, e);
            }
        }

        return new ChartDataResponse(query.ticker(), aggregatedPrices, benchmarks);
    }

    @Override
    public ReturnRateResponse calculateReturn(String ticker, ChartPeriod period) {
        validateStock(ticker);

        LocalDate end = LocalDate.now();
        LocalDate start = period.calculateStartDate(end);

        List<StockPriceResult> stockPrices = loadStockPricePort.loadPricesByTicker(ticker, start, end);
        if (stockPrices.isEmpty()) {
            throw new StockPriceException(ErrorCode.PRICE_DATA_NOT_FOUND);
        }

        String benchmarkTicker = resolveBenchmarkTicker(ticker);
        List<StockPriceResult> benchmarkPrices = loadBenchmarkPort.loadBenchmarkPrices(benchmarkTicker, start, end);

        BigDecimal stockReturn = calculateTotalReturn(stockPrices);
        BigDecimal benchmarkReturn = calculateTotalReturn(benchmarkPrices);

        return new ReturnRateResponse(ticker, period.getLabel(), stockReturn, benchmarkReturn);
    }

    private void validateStock(String ticker) {
        if (!loadStockPort.existsByTicker(ticker)) {
            throw new StockPriceException(ErrorCode.STOCK_NOT_FOUND);
        }
    }

    private String resolveBenchmarkTicker(String ticker) {
        // TODO: MarketType에 따른 벤치마크 매핑 로직 추가 (예: 나스닥 종목 -> ^IXIC)
        return DEFAULT_BENCHMARK;
    }

    private ChartPoint toChartPoint(StockPriceResult p) {
        return new ChartPoint(
                p.baseDate(),
                p.openPrice(),
                p.highPrice(),
                p.lowPrice(),
                p.closePrice(),
                p.adjClosePrice(),
                p.volume()
        );
    }

    private List<BenchmarkPoint> calculateBenchmarkReturns(List<StockPriceResult> daily) {
        if (daily.isEmpty()) return Collections.emptyList();
        
        BigDecimal firstPrice = daily.get(0).adjClosePrice();
        if (firstPrice.compareTo(BigDecimal.ZERO) == 0) return Collections.emptyList();

        return daily.stream()
                .map(p -> {
                    BigDecimal returnRate = p.adjClosePrice()
                            .subtract(firstPrice)
                            .divide(firstPrice, CALC_SCALE, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
                    return new BenchmarkPoint(p.baseDate(), returnRate);
                })
                .toList();
    }

    private BigDecimal calculateTotalReturn(List<StockPriceResult> prices) {
        if (prices.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal startPrice = prices.get(0).adjClosePrice();
        BigDecimal endPrice = prices.get(prices.size() - 1).adjClosePrice();
        
        if (startPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return endPrice.subtract(startPrice)
                .divide(startPrice, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
