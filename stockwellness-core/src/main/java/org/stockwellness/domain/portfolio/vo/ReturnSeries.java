package org.stockwellness.domain.portfolio.vo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.stockwellness.domain.portfolio.math.FinancialMath;

/**
 * 수익률 시계열 데이터를 관리하는 일급 컬렉션
 */
public record ReturnSeries(Map<LocalDate, BigDecimal> dailyReturns) {

    public ReturnSeries {
        // 날짜순 정렬 보장을 위해 TreeMap 사용
        dailyReturns = Collections.unmodifiableMap(new TreeMap<>(dailyReturns));
    }

    public boolean isEmpty() {
        return dailyReturns.isEmpty();
    }

    public int size() {
        return dailyReturns.size();
    }

    /**
     * 수익률 데이터 리스트만 추출합니다.
     */
    public List<BigDecimal> getReturnsOnly() {
        return dailyReturns.values().stream().toList();
    }

    /**
     * 표준편차(변동성)를 계산합니다.
     */
    public BigDecimal calculateVolatility() {
        return FinancialMath.calculateStandardDeviation(getReturnsOnly());
    }

    /**
     * 연환산 변동성을 계산합니다.
     */
    public BigDecimal calculateAnnualizedVolatility() {
        return FinancialMath.annualizeVolatility(calculateVolatility());
    }

    /**
     * 누적 수익률 흐름(가치 변화)으로부터 MDD를 계산합니다.
     * @param dailyValues 일일 자산 가치 흐름
     */
    public static BigDecimal calculateMDD(List<BigDecimal> dailyValues) {
        if (dailyValues == null || dailyValues.isEmpty()) return BigDecimal.ZERO;

        BigDecimal maxMDD = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;

        for (BigDecimal value : dailyValues) {
            if (value.compareTo(peak) > 0) peak = value;
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                // (Peak - Value) / Peak * 100
                BigDecimal drawdown = peak.subtract(value)
                        .divide(peak, new MathContext(16))
                        .multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(maxMDD) > 0) maxMDD = drawdown;
            }
        }
        return maxMDD;
    }
    
    /**
     * 수익률 시계열로부터 가치 흐름을 복원하여 MDD를 계산합니다.
     */
    public BigDecimal calculateMDDFromReturns() {
        if (dailyReturns.isEmpty()) return BigDecimal.ZERO;
        
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal current = BigDecimal.valueOf(100);
        values.add(current);
        
        for (BigDecimal r : dailyReturns.values()) {
            BigDecimal multiplier = BigDecimal.ONE.add(r.divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP));
            current = current.multiply(multiplier);
            values.add(current);
        }
        
        return calculateMDD(values);
    }
    
    /**
     * CAGR을 계산합니다.
     */
    public static BigDecimal calculateCAGR(BigDecimal start, BigDecimal end, double years) {
        return FinancialMath.calculateCAGR(start, end, years);
    }
}
