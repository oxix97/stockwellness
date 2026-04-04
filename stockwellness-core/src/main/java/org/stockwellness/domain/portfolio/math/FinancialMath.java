package org.stockwellness.domain.portfolio.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 정밀한 금융 수치 계산을 위한 BigDecimal 기반 유틸리티
 */
public class FinancialMath {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * (value ^ power) 연산을 수행합니다.
     * BigDecimal은 정수 승수만 지원하므로, 실수 승수는 Math.pow를 사용하되
     * 계산 전후로 유효 숫자를 최대한 유지합니다.
     */
    public static BigDecimal pow(BigDecimal value, double power) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (power == 1.0) return value;
        if (power == 0.0) return BigDecimal.ONE;
        
        return BigDecimal.valueOf(Math.pow(value.doubleValue(), power))
                .round(MC);
    }

    /**
     * 제곱근을 구합니다.
     */
    public static BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return value.sqrt(MC);
    }

    /**
     * 일일 변동성을 연환산 변동성으로 변환합니다 (252영업일 기준).
     */
    public static BigDecimal annualizeVolatility(BigDecimal dailyVolatility) {
        return dailyVolatility.multiply(sqrt(BigDecimal.valueOf(252))).round(MC);
    }

    /**
     * CAGR(연평균 복리 수익률)을 계산합니다.
     */
    public static BigDecimal calculateCAGR(BigDecimal startValue, BigDecimal endValue, double years) {
        if (startValue.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) return BigDecimal.ZERO;
        
        BigDecimal ratio = endValue.divide(startValue, MC);
        BigDecimal cagr = pow(ratio, 1.0 / years).subtract(BigDecimal.ONE).multiply(HUNDRED);
        
        return cagr.round(MC);
    }

    /**
     * 두 값의 변화율(%)을 계산합니다.
     */
    public static BigDecimal calculateReturnRate(BigDecimal start, BigDecimal end) {
        if (start.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return end.subtract(start).divide(start, MC).multiply(HUNDRED).round(MC);
    }

    /**
     * 표준편차를 계산합니다.
     */
    public static BigDecimal calculateStandardDeviation(java.util.List<BigDecimal> values) {
        if (values == null || values.size() < 2) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) sum = sum.add(v);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), MC);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(values.size()), MC);
        return sqrt(variance).round(MC);
    }

    /**
     * 두 데이터셋의 공분산을 계산합니다.
     */
    public static BigDecimal calculateCovariance(java.util.List<BigDecimal> values1, java.util.List<BigDecimal> values2) {
        if (values1 == null || values2 == null || values1.size() != values2.size() || values1.size() < 2) {
            return BigDecimal.ZERO;
        }

        int n = values1.size();
        BigDecimal sum1 = BigDecimal.ZERO;
        BigDecimal sum2 = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            sum1 = sum1.add(values1.get(i));
            sum2 = sum2.add(values2.get(i));
        }
        BigDecimal mean1 = sum1.divide(BigDecimal.valueOf(n), MC);
        BigDecimal mean2 = sum2.divide(BigDecimal.valueOf(n), MC);

        BigDecimal covSum = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            BigDecimal diff1 = values1.get(i).subtract(mean1);
            BigDecimal diff2 = values2.get(i).subtract(mean2);
            covSum = covSum.add(diff1.multiply(diff2));
        }

        return covSum.divide(BigDecimal.valueOf(n), MC).round(MC);
    }

    /**
     * 베타(Beta)를 계산합니다: Cov(Rp, Rm) / Var(Rm)
     */
    public static BigDecimal calculateBeta(java.util.List<BigDecimal> portfolioReturns, java.util.List<BigDecimal> benchmarkReturns) {
        if (portfolioReturns == null || benchmarkReturns == null || portfolioReturns.size() != benchmarkReturns.size() || portfolioReturns.isEmpty()) {
            return BigDecimal.ONE;
        }

        BigDecimal covariance = calculateCovariance(portfolioReturns, benchmarkReturns);
        
        // Variance of Rm
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : benchmarkReturns) sum = sum.add(v);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(benchmarkReturns.size()), MC);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal v : benchmarkReturns) {
            BigDecimal diff = v.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(benchmarkReturns.size()), MC);

        if (variance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;

        return covariance.divide(variance, MC).round(MC);
    }
}
