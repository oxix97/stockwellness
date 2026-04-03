package org.stockwellness.domain.portfolio.indicator;

import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.domain.portfolio.vo.ReturnSeries;
import org.stockwellness.domain.stock.BenchmarkType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 분리된 지표 계산기들을 사용하여 최종 BacktestResult를 조합하는 애그리게이터
 */
public class BacktestAggregator {

    private final PerformanceCalculator performanceCalculator = new PerformanceCalculator();
    private final RiskCalculator riskCalculator = new RiskCalculator();
    private final EfficiencyCalculator efficiencyCalculator = new EfficiencyCalculator();
    private final MarketCorrelationCalculator correlationCalculator = new MarketCorrelationCalculator();

    public BacktestResult aggregate(
            List<BacktestResult.DailyBacktestResult> dailyResults,
            IndicatorCalculator.IndicatorContext context,
            Map<String, BigDecimal> itemReturns,
            String aiComment
    ) {
        PerformanceCalculator.PerformanceMetrics perf = performanceCalculator.calculate(context);
        RiskCalculator.RiskMetrics risk = riskCalculator.calculate(context);
        EfficiencyCalculator.EfficiencyMetrics efficiency = efficiencyCalculator.calculate(context);
        Map<String, MarketCorrelationCalculator.CorrelationMetrics> correlations = correlationCalculator.calculate(context);

        List<BacktestResult.IndexComparison> comparisons = new ArrayList<>();
        for (Map.Entry<String, MarketCorrelationCalculator.CorrelationMetrics> entry : correlations.entrySet()) {
            String ticker = entry.getKey();
            MarketCorrelationCalculator.CorrelationMetrics metrics = entry.getValue();
            
            // 벤치마크별 MDD 및 상대 MDD 계산
            ReturnSeries benchmarkSeries = context.benchmarkReturns().get(ticker);
            BigDecimal benchmarkMdd = (benchmarkSeries != null) ? benchmarkSeries.calculateMDDFromReturns() : BigDecimal.ZERO;
            BigDecimal relativeMdd = risk.mdd().subtract(benchmarkMdd);

            comparisons.add(new BacktestResult.IndexComparison(
                BenchmarkType.fromTicker(ticker).getDescription(),
                ticker,
                metrics.indexReturn(),
                metrics.alpha(),
                metrics.beta(),
                benchmarkMdd,
                relativeMdd
            ));
        }

        // Primary Alpha/Beta/RelativeMDD (첫 번째 벤치마크 기준)
        BigDecimal primaryAlpha = comparisons.isEmpty() ? BigDecimal.ZERO : comparisons.get(0).alpha();
        BigDecimal primaryBeta = comparisons.isEmpty() ? BigDecimal.ONE : comparisons.get(0).beta();
        BigDecimal primaryRelativeMdd = comparisons.isEmpty() ? BigDecimal.ZERO : comparisons.get(0).relativeMdd();

        return new BacktestResult(
                dailyResults,
                perf.cagr(),
                risk.mdd(),
                primaryRelativeMdd,
                efficiency.sharpeRatio(),
                perf.totalReturnRate(),
                risk.annualizedVolatility(),
                primaryAlpha,
                primaryBeta,
                perf.bestYearRate(),
                perf.worstYearRate(),
                itemReturns,
                comparisons,
                aiComment
        );
    }
}
