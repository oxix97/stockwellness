package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 백테스트 결과(DailyBacktestResult)를 기반으로 성과 지표를 계산하는 유틸리티
 */
public class BacktestCalculator {

    private static final int SCALE = 6;
    private static final BigDecimal RF_RATE = new BigDecimal("0.02"); // 무위험 수익률 가정 (2%)

    public static BacktestResult calculate(List<BacktestResult.DailyBacktestResult> dailyResults) {
        if (dailyResults == null || dailyResults.isEmpty()) {
            return new BacktestResult(Collections.emptyList(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal firstValue = dailyResults.get(0).totalValue();
        BigDecimal lastValue = dailyResults.get(dailyResults.size() - 1).totalValue();
        
        // 1. 총 수익률
        BigDecimal totalReturnRate = lastValue.subtract(firstValue)
                .divide(firstValue, SCALE, RoundingMode.HALF_UP);

        // 2. CAGR (연평균 수익률)
        long days = ChronoUnit.DAYS.between(dailyResults.get(0).date(), dailyResults.get(dailyResults.size() - 1).date());
        double years = days / 365.25;
        BigDecimal cagr = BigDecimal.ZERO;
        if (years > 0) {
            double cagrDouble = Math.pow(lastValue.divide(firstValue, 10, RoundingMode.HALF_UP).doubleValue(), 1.0 / years) - 1;
            cagr = BigDecimal.valueOf(cagrDouble).setScale(SCALE, RoundingMode.HALF_UP);
        }

        // 3. MDD (최대 낙폭)
        BigDecimal mdd = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        for (var result : dailyResults) {
            if (result.totalValue().compareTo(peak) > 0) peak = result.totalValue();
            BigDecimal drop = result.totalValue().subtract(peak).divide(peak, SCALE, RoundingMode.HALF_UP);
            if (drop.compareTo(mdd) < 0) mdd = drop;
        }

        // 4. 변동성 (Volatility - 일일 수익률의 표준편차 * sqrt(252))
        List<BigDecimal> returns = dailyResults.stream()
                .map(BacktestResult.DailyBacktestResult::returnRate)
                .toList();
        BigDecimal dailyVol = calculateStandardDeviation(returns);
        BigDecimal annualVol = dailyVol.multiply(BigDecimal.valueOf(Math.sqrt(252))).setScale(SCALE, RoundingMode.HALF_UP);

        // 5. Sharpe Ratio (무위험 수익률 2% 가정)
        BigDecimal sharpe = BigDecimal.ZERO;
        if (annualVol.compareTo(BigDecimal.ZERO) > 0) {
            sharpe = cagr.subtract(RF_RATE).divide(annualVol, SCALE, RoundingMode.HALF_UP);
        }

        // 6. Best/Worst Year
        Map<Integer, BigDecimal> yearlyReturns = dailyResults.stream()
                .collect(Collectors.groupingBy(r -> r.date().getYear(),
                        Collectors.mapping(BacktestResult.DailyBacktestResult::returnRate, 
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)))); // 단순 합산 (실제는 기하수익률 권장하나 일단 단순화)
        
        BigDecimal bestYear = yearlyReturns.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal worstYear = yearlyReturns.values().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 7. Alpha/Beta (임시 0 처리, 필요 시 벤치마크 수익률과 공분산 계산 로직 추가 가능)
        BigDecimal alpha = BigDecimal.ZERO;
        BigDecimal beta = BigDecimal.ONE;

        return new BacktestResult(
                dailyResults,
                cagr,
                mdd,
                sharpe,
                totalReturnRate,
                annualVol,
                alpha,
                beta,
                bestYear,
                worstYear
        );
    }

    private static BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        if (values.size() <= 1) return BigDecimal.ZERO;
        double mean = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double sumSq = values.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                .sum();
        return BigDecimal.valueOf(Math.sqrt(sumSq / (values.size() - 1)));
    }
}
