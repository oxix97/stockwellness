package org.stockwellness.domain.portfolio.indicator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.portfolio.math.FinancialMath;

/**
 * 수익성 지표 계산기 (CAGR, 총 수익률 등)
 */
public class PerformanceCalculator implements IndicatorCalculator<PerformanceCalculator.PerformanceMetrics> {

    public record PerformanceMetrics(
        BigDecimal totalReturnRate,
        BigDecimal cagr,
        BigDecimal bestYearRate,
        BigDecimal worstYearRate
    ) {}

    @Override
    public PerformanceMetrics calculate(IndicatorContext context) {
        BigDecimal totalReturnRate = FinancialMath.calculateReturnRate(context.initialAmount(), context.finalAmount());
        BigDecimal cagr = FinancialMath.calculateCAGR(context.initialAmount(), context.finalAmount(), context.years());
        
        // Best/Worst Year Rate calculation
        List<LocalDate> dates = context.portfolioReturns().dailyReturns().keySet().stream().toList();
        if (dates.isEmpty()) return new PerformanceMetrics(totalReturnRate, cagr, BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal bestYearRate = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal worstYearRate = BigDecimal.valueOf(Double.MAX_VALUE);

        Map<Integer, List<Integer>> indicesByYear = new HashMap<>();
        // dailyValues[0] is initial. dailyValues[1] is Day 1 value.
        // dates[0] is Day 1.
        for (int i = 0; i < dates.size(); i++) {
            indicesByYear.computeIfAbsent(dates.get(i).getYear(), k -> new ArrayList<>()).add(i);
        }

        for (List<Integer> indices : indicesByYear.values()) {
            if (indices.isEmpty()) continue;
            
            // For a year, start value is the value BEFORE the first return of that year.
            // i.e., Day (firstIndex) - 1.
            int firstIdx = indices.getFirst();
            int lastIdx = indices.getLast();
            
            BigDecimal yearStartValue = context.dailyValues().get(firstIdx); // Value at end of previous day
            BigDecimal yearEndValue = context.dailyValues().get(lastIdx + 1); // Value at end of this day
            
            BigDecimal yearRate = FinancialMath.calculateReturnRate(yearStartValue, yearEndValue);

            if (yearRate.compareTo(bestYearRate) > 0) bestYearRate = yearRate;
            if (yearRate.compareTo(worstYearRate) < 0) worstYearRate = yearRate;
        }

        if (bestYearRate.doubleValue() == -Double.MAX_VALUE) bestYearRate = BigDecimal.ZERO;
        if (worstYearRate.doubleValue() == Double.MAX_VALUE) worstYearRate = BigDecimal.ZERO;

        return new PerformanceMetrics(totalReturnRate, cagr, bestYearRate, worstYearRate);
    }
}
