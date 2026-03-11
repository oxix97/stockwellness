package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class PortfolioStatCalculator {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

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

        BigDecimal mean = calculateMean(returns);
        BigDecimal variance = calculateVariance(returns, mean);
        BigDecimal stdDev = sqrt(variance);

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return mean.divide(stdDev, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBeta(List<BigDecimal> portfolioReturns, List<BigDecimal> marketReturns) {
        if (portfolioReturns == null || marketReturns == null || portfolioReturns.size() != marketReturns.size() || portfolioReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal pMean = calculateMean(portfolioReturns);
        BigDecimal mMean = calculateMean(marketReturns);

        BigDecimal covariance = BigDecimal.ZERO;
        BigDecimal mVariance = BigDecimal.ZERO;

        for (int i = 0; i < portfolioReturns.size(); i++) {
            BigDecimal pDiff = portfolioReturns.get(i).subtract(pMean);
            BigDecimal mDiff = marketReturns.get(i).subtract(mMean);
            
            covariance = covariance.add(pDiff.multiply(mDiff, MC));
            mVariance = mVariance.add(mDiff.multiply(mDiff, MC));
        }

        if (mVariance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return covariance.divide(mVariance, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMean(List<BigDecimal> data) {
        BigDecimal sum = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(data.size()), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVariance(List<BigDecimal> data, BigDecimal mean) {
        BigDecimal sumSqDiff = BigDecimal.ZERO;
        for (BigDecimal d : data) {
            BigDecimal diff = d.subtract(mean);
            sumSqDiff = sumSqDiff.add(diff.multiply(diff, MC));
        }
        return sumSqDiff.divide(BigDecimal.valueOf(data.size()), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return value.sqrt(MC);
    }
}
