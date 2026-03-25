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
                BigDecimal drawdown = peak.subtract(res.totalValue()).divide(peak, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(maxMDD) > 0) maxMDD = drawdown;
            }
        }

        // 3. 변동성 (표준편차)
        List<BigDecimal> dailyReturns = dailyResults.stream()
                .map(BacktestResult.DailyBacktestResult::returnRate)
                .toList();
        BigDecimal volatility = calculateStandardDeviation(dailyReturns);

        // 4. CAGR (연평균 수익률)
        double years = dailyResults.size() / 252.0; // 영업일 기준 약 252일
        BigDecimal cagr = calculateCAGR(first.totalInvested(), last.totalValue(), years);

        // 5. 다중 지수 비교 결과 산출
        List<BacktestResult.IndexComparison> comparisons = new ArrayList<>();
        Map<String, BigDecimal> lastBenchmarkReturns = last.benchmarkReturnRates();
        
        for (Map.Entry<String, BigDecimal> entry : lastBenchmarkReturns.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal indexReturn = entry.getValue();
            
            // Alpha, Beta 간이 계산 (Beta는 수익률 상관관계로 계산하는 것이 정석이나 여기선 단순화)
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

        // 6. 메인 지표 (기본적으로 KOSPI 기준 혹은 첫 번째 지수 기준)
        BigDecimal primaryAlpha = comparisons.isEmpty() ? BigDecimal.ZERO : comparisons.get(0).alpha();
        BigDecimal primaryBeta = comparisons.isEmpty() ? BigDecimal.ONE : comparisons.get(0).beta();

        return new BacktestResult(
                dailyResults,
                cagr,
                maxMDD,
                BigDecimal.ZERO, // Sharpe Ratio (추후 고도화)
                totalReturnRate,
                volatility,
                primaryAlpha,
                primaryBeta,
                BigDecimal.ZERO, BigDecimal.ZERO, // Best/Worst Year
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
