package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PortfolioStatCalculator {

    public BigDecimal calculateMDD(List<BigDecimal> values) {
        if (values == null || values.size() < 2) return BigDecimal.ZERO;

        BigDecimal peak = values.get(0);
        BigDecimal maxDropRatio = BigDecimal.ZERO;

        for (BigDecimal value : values) {
            if (value.compareTo(peak) > 0) {
                peak = value;
            } else {
                BigDecimal dropRatio = peak.subtract(value).divide(peak, 6, RoundingMode.HALF_UP);
                if (dropRatio.compareTo(maxDropRatio) > 0) {
                    maxDropRatio = dropRatio;
                }
            }
        }

        return maxDropRatio.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSharpeRatio(List<BigDecimal> returns) {
        if (returns == null || returns.isEmpty()) return BigDecimal.ZERO;

        double[] data = returns.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double mean = calculateMean(data);
        double stdDev = calculateStdDev(data, mean);

        if (stdDev == 0) return BigDecimal.ZERO;

        return BigDecimal.valueOf(mean / stdDev).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBeta(List<BigDecimal> portfolioReturns, List<BigDecimal> marketReturns) {
        if (portfolioReturns == null || marketReturns == null || portfolioReturns.size() != marketReturns.size() || portfolioReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double[] p = portfolioReturns.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double[] m = marketReturns.stream().mapToDouble(BigDecimal::doubleValue).toArray();

        double mMean = calculateMean(m);
        double pMean = calculateMean(p);

        double covariance = 0;
        double mVariance = 0;

        for (int i = 0; i < p.length; i++) {
            covariance += (p[i] - pMean) * (m[i] - mMean);
            mVariance += Math.pow(m[i] - mMean, 2);
        }

        if (mVariance == 0) return BigDecimal.ZERO;

        return BigDecimal.valueOf(covariance / mVariance).setScale(4, RoundingMode.HALF_UP);
    }

    private double calculateMean(double[] data) {
        double sum = 0;
        for (double d : data) sum += d;
        return sum / data.length;
    }

    private double calculateStdDev(double[] data, double mean) {
        double sum = 0;
        for (double d : data) sum += Math.pow(d - mean, 2);
        return Math.sqrt(sum / data.length);
    }
}
