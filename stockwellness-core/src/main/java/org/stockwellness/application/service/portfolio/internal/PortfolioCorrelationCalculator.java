package org.stockwellness.application.service.portfolio.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioCorrelationCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;

    public BigDecimal calculateCorrelation(List<BigDecimal> returnsA, List<BigDecimal> returnsB) {
        if (returnsA == null || returnsB == null || returnsA.size() != returnsB.size() || returnsA.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal aMean = calculateMean(returnsA);
        BigDecimal bMean = calculateMean(returnsB);

        BigDecimal covariance = BigDecimal.ZERO;
        BigDecimal aVariance = BigDecimal.ZERO;
        BigDecimal bVariance = BigDecimal.ZERO;

        for (int i = 0; i < returnsA.size(); i++) {
            BigDecimal aDiff = returnsA.get(i).subtract(aMean);
            BigDecimal bDiff = returnsB.get(i).subtract(bMean);
            
            covariance = covariance.add(aDiff.multiply(bDiff, MC));
            aVariance = aVariance.add(aDiff.pow(2, MC));
            bVariance = bVariance.add(bDiff.pow(2, MC));
        }

        BigDecimal varianceProduct = aVariance.multiply(bVariance, MC);
        if (varianceProduct.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal denominator = varianceProduct.sqrt(MC);
        
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return covariance.divide(denominator, 4, RoundingMode.HALF_UP);
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

    private BigDecimal calculateMean(List<BigDecimal> data) {
        BigDecimal sum = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(data.size()), MC);
    }
}
