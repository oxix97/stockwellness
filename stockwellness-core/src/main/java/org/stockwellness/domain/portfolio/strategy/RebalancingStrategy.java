package org.stockwellness.domain.portfolio.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

import org.stockwellness.domain.portfolio.RebalancingPeriod;

/**
 * 포트폴리오 비중 재조정(Rebalancing) 전략
 */
public class RebalancingStrategy {

    public boolean shouldRebalance(LocalDate currentDate, LocalDate lastRebalanceDate, RebalancingPeriod period) {
        if (period == null || period == RebalancingPeriod.NONE) return false;
        
        return switch (period) {
            case MONTHLY -> currentDate.getMonthValue() != lastRebalanceDate.getMonthValue();
            case QUARTERLY -> (currentDate.getMonthValue() - 1) / 3 != (lastRebalanceDate.getMonthValue() - 1) / 3;
            case YEARLY -> currentDate.getYear() != lastRebalanceDate.getYear();
            default -> false;
        };
    }

    /**
     * 현재 자산 가치를 기준으로 목표 비중에 맞게 보유 수량을 조정합니다.
     */
    public void rebalance(Map<String, BigDecimal> shares, BigDecimal totalValue, Map<String, BigDecimal> targetWeights, Map<String, BigDecimal> currentPrices) {
        for (Map.Entry<String, BigDecimal> entry : targetWeights.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();
            
            BigDecimal targetAssetValue = totalValue.multiply(weight).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            BigDecimal price = currentPrices.getOrDefault(symbol, BigDecimal.ZERO);
            
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                shares.put(symbol, targetAssetValue.divide(price, 8, RoundingMode.HALF_UP));
            } else {
                shares.put(symbol, BigDecimal.ZERO);
            }
        }
    }
}
