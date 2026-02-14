package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.CalculateReturnUseCase;
import org.stockwellness.application.port.in.stock.LoadChartDataUseCase;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse.BenchmarkPoint;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse.ChartPoint;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.LoadStockPricePort;
import org.stockwellness.domain.stock.ChartPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockChartService implements LoadChartDataUseCase, CalculateReturnUseCase {

    private final LoadStockPricePort loadStockPricePort;
    private final LoadBenchmarkPort loadBenchmarkPort;

    @Override
    public ChartDataResponse loadChartData(ChartQuery query) {
        LocalDate end = LocalDate.now();
        LocalDate start = query.period().calculateStartDate(end);

        List<StockPriceResult> dailyPrices = loadStockPricePort.loadPricesByTicker(query.ticker(), start, end);
        
        List<ChartPoint> aggregatedPrices = switch (query.frequency()) {
            case WEEKLY -> PriceDataAggregator.aggregateToWeekly(dailyPrices);
            case MONTHLY -> PriceDataAggregator.aggregateToMonthly(dailyPrices);
            case DAILY -> dailyPrices.stream().map(this::toChartPoint).toList();
        };

        List<BenchmarkPoint> benchmarks = Collections.emptyList();
        if (query.includeBenchmark()) {
            String benchmarkTicker = "^KS11";
            List<StockPriceResult> benchmarkDaily = loadBenchmarkPort.loadBenchmarkPrices(benchmarkTicker, start, end);
            benchmarks = calculateBenchmarkReturns(benchmarkDaily);
        }

        return new ChartDataResponse(query.ticker(), aggregatedPrices, benchmarks);
    }

    @Override
    public ReturnRateResponse calculateReturn(String ticker, ChartPeriod period) {
        LocalDate end = LocalDate.now();
        LocalDate start = period.calculateStartDate(end);

        List<StockPriceResult> stockPrices = loadStockPricePort.loadPricesByTicker(ticker, start, end);
        List<StockPriceResult> benchmarkPrices = loadBenchmarkPort.loadBenchmarkPrices("^KS11", start, end);

        BigDecimal stockReturn = calculateTotalReturn(stockPrices);
        BigDecimal benchmarkReturn = calculateTotalReturn(benchmarkPrices);

        return new ReturnRateResponse(ticker, period.getLabel(), stockReturn, benchmarkReturn);
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
                            .divide(firstPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
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
                .divide(startPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
