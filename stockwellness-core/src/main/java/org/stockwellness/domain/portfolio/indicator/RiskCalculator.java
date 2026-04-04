package org.stockwellness.domain.portfolio.indicator;

import org.stockwellness.domain.portfolio.vo.ReturnSeries;
import java.math.BigDecimal;

/**
 * 위험도 지표 계산기 (MDD, 변동성 등)
 */
public class RiskCalculator implements IndicatorCalculator<RiskCalculator.RiskMetrics> {

    public record RiskMetrics(
        BigDecimal mdd,
        BigDecimal annualizedVolatility,
        BigDecimal relativeMdd
    ) {}

    @Override
    public RiskMetrics calculate(IndicatorContext context) {
        BigDecimal portfolioMdd = ReturnSeries.calculateMDD(context.dailyValues());
        BigDecimal annualizedVolatility = context.portfolioReturns().calculateAnnualizedVolatility();
        
        // 벤치마크 MDD 계산 및 상대 MDD 산출
        BigDecimal relativeMdd = BigDecimal.ZERO;
        if (context.primaryBenchmarkTicker() != null && context.benchmarkReturns().containsKey(context.primaryBenchmarkTicker())) {
            ReturnSeries benchmark = context.benchmarkReturns().get(context.primaryBenchmarkTicker());
            BigDecimal benchmarkMdd = benchmark.calculateMDDFromReturns();
            relativeMdd = portfolioMdd.subtract(benchmarkMdd);
        }
        
        return new RiskMetrics(portfolioMdd, annualizedVolatility, relativeMdd);
    }
}
