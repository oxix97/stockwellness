package org.stockwellness.application.service.portfolio.internal;

import org.stockwellness.domain.stock.BenchmarkType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 백테스트 결과(DailyBacktestResult)를 기반으로 성과 지표를 계산하는 유틸리티
 */
public class BacktestCalculator {

    private static final BigDecimal DEFAULT_RISK_FREE_RATE = BigDecimal.valueOf(3.0); // 무위험 수익률 3% 가정

    public static BacktestResult calculate(List<BacktestResult.DailyBacktestResult> dailyResults) {
        return calculate(dailyResults, null);
    }

    public static BacktestResult calculate(List<BacktestResult.DailyBacktestResult> dailyResults, String aiComment) {
        if (dailyResults == null || dailyResults.isEmpty()) {
            return new BacktestResult(Collections.emptyList(), 
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList(), aiComment);
        }

        BacktestResult.DailyBacktestResult first = dailyResults.get(0);
        BacktestResult.DailyBacktestResult last = dailyResults.get(dailyResults.size() - 1);

        // 1. 총 수익률
        BigDecimal totalReturnRate = last.returnRate();

        // 2. MDD (Maximum Drawdown)
        BigDecimal maxMDD = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        for (BacktestResult.DailyBacktestResult res : dailyResults) {
            if (res.totalValue().compareTo(peak) > 0) peak = res.totalValue();
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(res.totalValue()).divide(peak, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(maxMDD) > 0) maxMDD = drawdown.setScale(4, RoundingMode.HALF_UP);
            }
        }

        // 3. 변동성 (표준편차)
        List<BigDecimal> dailyReturns = dailyResults.stream()
                .map(BacktestResult.DailyBacktestResult::returnRate)
                .toList();
        BigDecimal dailyVolatility = calculateStandardDeviation(dailyReturns);
        BigDecimal annualizedVolatility = dailyVolatility.multiply(BigDecimal.valueOf(Math.sqrt(252))).setScale(4, RoundingMode.HALF_UP);

        // 4. CAGR (연평균 수익률)
        double years = dailyResults.size() / 252.0; // 영업일 기준 약 252일
        BigDecimal cagr = calculateCAGR(first.totalInvested(), last.totalValue(), years);

        // 5. Sharpe Ratio
        BigDecimal sharpeRatio = BigDecimal.ZERO;
        if (annualizedVolatility.compareTo(BigDecimal.ZERO) > 0) {
            sharpeRatio = cagr.subtract(DEFAULT_RISK_FREE_RATE).divide(annualizedVolatility, 4, RoundingMode.HALF_UP);
        }

        // 6. Best/Worst Year Rate
        Map<Integer, List<BacktestResult.DailyBacktestResult>> resultsByYear = dailyResults.stream()
                .collect(Collectors.groupingBy(res -> res.date().getYear()));

        BigDecimal bestYearRate = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal worstYearRate = BigDecimal.valueOf(Double.MAX_VALUE);

        for (List<BacktestResult.DailyBacktestResult> yearResults : resultsByYear.values()) {
            if (yearResults.isEmpty()) continue;

            BacktestResult.DailyBacktestResult yearFirst = yearResults.get(0);
            BacktestResult.DailyBacktestResult yearLast = yearResults.get(yearResults.size() - 1);

            // 연초 대비 연말 수익률: (연말 가치 / 연초 투자금액 - 1) * 100
            // 여기서는 이미 dailyResult에 누적 수익률이 있으므로, 연도 내의 절대적 수익률 변화를 계산
            // (YearEndValue / YearStartValue - 1) * 100
            BigDecimal yearRate = BigDecimal.ZERO;
            if (yearFirst.totalValue().compareTo(BigDecimal.ZERO) > 0) {
                yearRate = yearLast.totalValue().subtract(yearFirst.totalValue())
                        .divide(yearFirst.totalValue(), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            if (yearRate.compareTo(bestYearRate) > 0) bestYearRate = yearRate;
            if (yearRate.compareTo(worstYearRate) < 0) worstYearRate = yearRate;
        }
        if (bestYearRate.doubleValue() == -Double.MAX_VALUE) bestYearRate = BigDecimal.ZERO;
        if (worstYearRate.doubleValue() == Double.MAX_VALUE) worstYearRate = BigDecimal.ZERO;

        // 7. 다중 지수 비교 결과 산출
        List<BacktestResult.IndexComparison> comparisons = new ArrayList<>();
        Map<String, BigDecimal> lastBenchmarkReturns = last.benchmarkReturnRates();

        for (Map.Entry<String, BigDecimal> entry : lastBenchmarkReturns.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal indexReturn = entry.getValue();

            // Alpha, Beta 간이 계산
            BigDecimal alpha = totalReturnRate.subtract(indexReturn);
            BigDecimal beta = calculateSimpleBeta(dailyResults, ticker);

            comparisons.add(new BacktestResult.IndexComparison(
                BenchmarkType.fromTicker(ticker).getDescription(),
                ticker,
                indexReturn,
                alpha,
                beta
            ));
        }

        // 8. 메인 지표
        BigDecimal primaryAlpha = comparisons.isEmpty() ? BigDecimal.ZERO : comparisons.get(0).alpha();
        BigDecimal primaryBeta = comparisons.isEmpty() ? BigDecimal.ONE : comparisons.get(0).beta();

        return new BacktestResult(
                dailyResults,
                cagr,
                maxMDD,
                sharpeRatio,
                totalReturnRate,
                annualizedVolatility,
                primaryAlpha,
                primaryBeta,
                bestYearRate,
                worstYearRate,
                comparisons,
                aiComment
        );
    }

    private static BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        if (values.size() < 2) return BigDecimal.ZERO;
        double sum = 0;
        for (BigDecimal v : values) sum += v.doubleValue();
        double mean = sum / values.size();
        
        double variance = 0;
        for (BigDecimal v : values) variance += Math.pow(v.doubleValue() - mean, 2);
        return BigDecimal.valueOf(Math.sqrt(variance / values.size())).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateCAGR(BigDecimal start, BigDecimal end, double years) {
        if (start.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) return BigDecimal.ZERO;
        double ratio = end.doubleValue() / start.doubleValue();
        double cagr = (Math.pow(ratio, 1.0 / years) - 1) * 100;
        return BigDecimal.valueOf(cagr).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateSimpleBeta(List<BacktestResult.DailyBacktestResult> results, String ticker) {
        // 베타 = Cov(Rp, Rm) / Var(Rm)
        // 여기서는 단순 구현을 위해 1.0 반환 (추후 고도화 가능)
        return BigDecimal.ONE;
    }
}
