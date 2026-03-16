package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.diagnosis.type.BalanceScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.CashScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PortfolioHealthCalculator {

    private final PortfolioStatCalculator statCalculator = new PortfolioStatCalculator();

    public CalculatedHealth calculate(DiagnosisContext context) {
        Portfolio portfolio = context.portfolio();
        Map<String, Stock> stockMap = context.stockMap();
        Map<String, List<StockPrice>> stockPriceMap = context.stockPriceMap();

        BigDecimal totalPurchaseAmount = portfolio.calculateTotalPurchaseAmount();
        if (totalPurchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
            return new CalculatedHealth(0, Collections.emptyMap(), new PortfolioAiContext.RiskMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        double weightedDefense = 0;
        double weightedAttack = 0;
        double weightedEndurance = 0;
        double weightedAgility = 0;

        // 포트폴리오 통합 지표 (가중 평균)
        BigDecimal totalSharpe = BigDecimal.ZERO;
        BigDecimal totalMdd = BigDecimal.ZERO;
        BigDecimal totalVol = BigDecimal.ZERO;

        int stockCount = 0;
        Set<MarketType> marketTypes = new HashSet<>();

        for (PortfolioItem item : portfolio.getItems()) {
            BigDecimal itemPurchaseAmount = item.calculatePurchaseAmount();
            BigDecimal weight = itemPurchaseAmount.divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP);
            double weightDouble = weight.doubleValue();

            if (item.getAssetType() == AssetType.STOCK) {
                stockCount++;
                Stock stock = Optional.ofNullable(stockMap.get(item.getSymbol()))
                        .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + item.getSymbol()));

                marketTypes.add(stock.getMarketType());

                List<StockPrice> histories = stockPriceMap.getOrDefault(item.getSymbol(), List.of());
                if (!histories.isEmpty()) {
                    List<BigDecimal> prices = histories.stream().map(StockPrice::getClosePrice).toList();
                    List<BigDecimal> returns = calculateReturns(prices);

                    // 1. 공격성 (Sharpe Ratio 기반)
                    BigDecimal sharpe = statCalculator.calculateSharpeRatio(returns, BigDecimal.ZERO);
                    weightedAttack += normalizeSharpe(sharpe) * weightDouble;
                    totalSharpe = totalSharpe.add(sharpe.multiply(weight));

                    // 2. 방어성 (MDD 기반)
                    BigDecimal mdd = statCalculator.calculateMDD(prices);
                    weightedDefense += normalizeMDD(mdd) * weightDouble;
                    totalMdd = totalMdd.add(mdd.multiply(weight));

                    // 3. 인내심 (변동성 기반)
                    BigDecimal volatility = statCalculator.calculateVolatility(returns);
                    weightedEndurance += normalizeVolatility(volatility) * weightDouble;
                    totalVol = totalVol.add(volatility.multiply(weight));

                    // 4. 민첩성
                    weightedAgility += 70 * weightDouble; 
                }
            } else if (item.getAssetType() == AssetType.CASH) {
                weightedDefense += CashScorePolicy.DEFENSE.getScore() * weightDouble;
                weightedAttack += CashScorePolicy.ATTACK.getScore() * weightDouble;
                weightedEndurance += CashScorePolicy.ENDURANCE.getScore() * weightDouble;
                weightedAgility += CashScorePolicy.AGILITY.getScore() * weightDouble;
            }
        }

        int balanceScore = BalanceScorePolicy.calculate(stockCount, marketTypes);

        Map<String, Integer> categories = new HashMap<>();
        categories.put(DiagnosisCategory.DEFENSE.getKey(), (int) Math.round(weightedDefense));
        categories.put(DiagnosisCategory.ATTACK.getKey(), (int) Math.round(weightedAttack));
        categories.put(DiagnosisCategory.ENDURANCE.getKey(), (int) Math.round(weightedEndurance));
        categories.put(DiagnosisCategory.AGILITY.getKey(), (int) Math.round(weightedAgility));
        categories.put(DiagnosisCategory.BALANCE.getKey(), balanceScore);

        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int overallScore = (int) Math.round(average);

        return new CalculatedHealth(
                overallScore, 
                categories, 
                new PortfolioAiContext.RiskMetrics(totalSharpe, totalMdd, totalVol)
        );
    }

    private List<BigDecimal> calculateReturns(List<BigDecimal> prices) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal prev = prices.get(i - 1);
            BigDecimal curr = prices.get(i);
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(curr.subtract(prev).divide(prev, 10, RoundingMode.HALF_UP));
            }
        }
        return returns;
    }

    private double normalizeSharpe(BigDecimal sharpe) {
        double val = sharpe.doubleValue() * 50;
        return Math.min(100, Math.max(0, val));
    }

    private double normalizeMDD(BigDecimal mdd) {
        double val = 100 - (mdd.doubleValue() * 2);
        return Math.min(100, Math.max(0, val));
    }

    private double normalizeVolatility(BigDecimal vol) {
        double val = 100 - (vol.doubleValue() * 2000);
        return Math.min(100, Math.max(0, val));
    }
}
