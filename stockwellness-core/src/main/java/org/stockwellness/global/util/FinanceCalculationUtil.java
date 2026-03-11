package org.stockwellness.global.util;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 순수 금융 수치 계산을 위한 유틸리티
 */
public class FinanceCalculationUtil {

    /**
     * 두 수의 비율(%)을 소수점 4자리까지 정밀 계산합니다.
     */
    public static BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 시세 리스트로부터 일일 수익률 시계열 데이터를 추출합니다.
     */
    public static List<BigDecimal> calculateDailyReturns(List<StockPriceResult> prices) {
        if (prices == null || prices.size() < 2) return List.of();
        
        List<BigDecimal> sorted = prices.stream()
                .sorted(Comparator.comparing(StockPriceResult::baseDate))
                .map(StockPriceResult::closePrice)
                .toList();
        
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            BigDecimal prev = sorted.get(i - 1);
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(sorted.get(i).subtract(prev)
                        .divide(prev, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
        }
        return returns;
    }
}
