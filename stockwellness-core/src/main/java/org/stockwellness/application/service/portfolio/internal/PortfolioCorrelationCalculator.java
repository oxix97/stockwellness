package org.stockwellness.application.service.portfolio.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioCorrelationCalculator {

    public BigDecimal calculateCorrelation(List<BigDecimal> returnsA, List<BigDecimal> returnsB) {
        if (returnsA == null || returnsB == null || returnsA.size() != returnsB.size() || returnsA.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double[] a = returnsA.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double[] b = returnsB.stream().mapToDouble(BigDecimal::doubleValue).toArray();

        double aMean = calculateMean(a);
        double bMean = calculateMean(b);

        double covariance = 0;
        double aVariance = 0;
        double bVariance = 0;

        for (int i = 0; i < a.length; i++) {
            double aDiff = a[i] - aMean;
            double bDiff = b[i] - bMean;
            covariance += aDiff * bDiff;
            aVariance += Math.pow(aDiff, 2);
            bVariance += Math.pow(bDiff, 2);
        }

        double denominator = Math.sqrt(aVariance * bVariance);
        if (denominator == 0) return BigDecimal.ZERO;

        return BigDecimal.valueOf(covariance / denominator).setScale(4, RoundingMode.HALF_UP);
    }

    public Map<String, Map<String, BigDecimal>> calculateMatrix(Map<String, List<BigDecimal>> returnsMap) {
        Map<String, Map<String, BigDecimal>> matrix = new HashMap<>();
        List<String> symbols = returnsMap.keySet().stream().sorted().toList();

        for (String s1 : symbols) {
            matrix.put(s1, new HashMap<>());
            for (String s2 : symbols) {
                if (s1.equals(s2)) {
                    matrix.get(s1).put(s2, BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                } else if (matrix.containsKey(s2) && matrix.get(s2).containsKey(s1)) {
                    matrix.get(s1).put(s2, matrix.get(s2).get(s1));
                } else {
                    matrix.get(s1).put(s2, calculateCorrelation(returnsMap.get(s1), returnsMap.get(s2)));
                }
            }
        }

        return matrix;
    }

    private double calculateMean(double[] data) {
        double sum = 0;
        for (double d : data) sum += d;
        return sum / data.length;
    }
}
