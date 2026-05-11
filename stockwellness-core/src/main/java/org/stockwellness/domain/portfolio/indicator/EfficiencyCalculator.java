package org.stockwellness.domain.portfolio.indicator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.stockwellness.domain.portfolio.math.FinancialMath;
import org.stockwellness.domain.portfolio.vo.ReturnSeries;

/**
 * 효율성 지표 계산기 (Sharpe Ratio, Sortino Ratio 등)
 */
public class EfficiencyCalculator implements IndicatorCalculator<EfficiencyCalculator.EfficiencyMetrics> {

    public record EfficiencyMetrics(
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio
    ) {}

    private static final BigDecimal VOLATILITY_THRESHOLD = new BigDecimal("0.00000001");

    @Override
    public EfficiencyMetrics calculate(IndicatorContext context) {
        BigDecimal riskFreeRate = context.riskFreeRate();
        
        // 1. 초과 수익률(Excess Return) 계산을 위한 기준 수익률 결정
        // 벤치마크(KOSPI 등)가 지정되어 있으면 벤치마크의 CAGR을 사용하고, 없으면 무위험 수익률(riskFreeRate)을 사용
        BigDecimal baseReturn = riskFreeRate;
        if (context.primaryBenchmarkTicker() != null && context.benchmarkReturns().containsKey(context.primaryBenchmarkTicker())) {
            ReturnSeries benchmark = context.benchmarkReturns().get(context.primaryBenchmarkTicker());
            baseReturn = calculateCAGRFromSeries(benchmark, context.years());
        }
        
        // 포트폴리오 CAGR - 기준 수익률
        BigDecimal cagr = context.portfolioCagr() != null ? context.portfolioCagr() : 
                FinancialMath.calculateCAGR(context.initialAmount(), context.finalAmount(), context.years());
        BigDecimal excessReturn = cagr.subtract(baseReturn);

        // 2. Sharpe Ratio (고도화: 벤치마크 대비 상대 Sharpe 지표 성격 포함)
        BigDecimal annualizedVolatility = context.portfolioReturns().calculateAnnualizedVolatility();
        BigDecimal sharpeRatio = BigDecimal.ZERO;
        if (annualizedVolatility.compareTo(VOLATILITY_THRESHOLD) > 0) {
            sharpeRatio = excessReturn.divide(annualizedVolatility, 16, RoundingMode.HALF_UP);
        }

        // 3. Sortino Ratio
        BigDecimal downsideVolatility = calculateDownsideVolatility(context.portfolioReturns().getReturnsOnly());
        BigDecimal annualizedDownsideVolatility = FinancialMath.annualizeVolatility(downsideVolatility);
        BigDecimal sortinoRatio = BigDecimal.ZERO;
        if (annualizedDownsideVolatility.compareTo(VOLATILITY_THRESHOLD) > 0) {
            sortinoRatio = excessReturn.divide(annualizedDownsideVolatility, 16, RoundingMode.HALF_UP);
        }

        return new EfficiencyMetrics(sharpeRatio, sortinoRatio);
    }

    private BigDecimal calculateCAGRFromSeries(ReturnSeries series, double years) {
        if (series == null || series.isEmpty()) return BigDecimal.ZERO;
        
        // 누적 수익률 계산
        BigDecimal cumulative = BigDecimal.ONE;
        for (BigDecimal r : series.getReturnsOnly()) {
            BigDecimal multiplier = BigDecimal.ONE.add(r.divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP));
            cumulative = cumulative.multiply(multiplier, new MathContext(16));
        }
        
        // 누적 수익률을 바탕으로 CAGR 역산
        return FinancialMath.calculateCAGR(BigDecimal.ONE, cumulative, years);
    }

    private BigDecimal calculateDownsideVolatility(List<BigDecimal> returns) {
        if (returns == null || returns.isEmpty()) return BigDecimal.ZERO;
        
        // Downside returns only (< 0)
        List<BigDecimal> negativeReturns = returns.stream()
                .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .toList();

        if (negativeReturns.isEmpty()) return BigDecimal.ZERO;

        // Variance of negative returns (assuming MAR = 0)
        BigDecimal sumSquared = BigDecimal.ZERO;
        for (BigDecimal r : negativeReturns) {
            sumSquared = sumSquared.add(r.multiply(r));
        }
        
        // Sum / TOTAL returns count (not just negative returns count)
        BigDecimal downsideVariance = sumSquared.divide(BigDecimal.valueOf(returns.size()), 16, RoundingMode.HALF_UP);
        return FinancialMath.sqrt(downsideVariance);
    }
}
