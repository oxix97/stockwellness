package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.stock.MarketType;

import java.util.Arrays;
import java.util.Set;

public class BalanceScorePolicy {

    @Getter
    @RequiredArgsConstructor
    public enum StockCount {
        DIVERSIFIED(4, 100),
        MODERATE(2, 60),
        CONCENTRATED(1, 20),
        EMPTY(0, 0);

        private final int minCount;
        private final int score;

        public static int getScore(int count) {
            return Arrays.stream(values())
                    .filter(policy -> count >= policy.minCount)
                    .findFirst()
                    .map(StockCount::getScore)
                    .orElse(EMPTY.score);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum MarketDispersion {
        MIXED(100),
        SINGLE(50);

        private final int score;

        public static int getScore(Set<MarketType> marketTypes) {
            if (marketTypes.contains(MarketType.KOSPI) && marketTypes.contains(MarketType.KOSDAQ)) {
                return MIXED.score;
            }
            return SINGLE.score;
        }
    }
    
    public static int calculate(int stockCount, Set<MarketType> marketTypes) {
        return (StockCount.getScore(stockCount) + MarketDispersion.getScore(marketTypes)) / 2;
    }
}
