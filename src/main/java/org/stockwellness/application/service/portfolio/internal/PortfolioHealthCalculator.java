package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.diagnosis.type.BalanceScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.CashScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockPrice;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PortfolioHealthCalculator {

//    private final StockStatCalculator stockStatCalculator;

    public CalculatedHealth calculate(DiagnosisContext context) {
        Portfolio portfolio = context.portfolio();
        Map<String, Stock> stockMap = context.stockMap();
        Map<String, List<StockPrice>> stockPriceMap = context.stockPriceMap();

        double totalDefense = 0;
        double totalAttack = 0;
        double totalEndurance = 0;
        double totalAgility = 0;

        int stockCount = 0;
        Set<MarketType> marketTypes = new HashSet<>();

        for (PortfolioItem item : portfolio.getItems()) {
            double weight = (double) item.getPieceCount() / Portfolio.MAX_PIECES;

            if (item.getAssetType() == AssetType.STOCK) {
                stockCount++;
                Stock stock = Optional.ofNullable(stockMap.get(item.getIsinCode()))
                        .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + item.getIsinCode()));

                marketTypes.add(stock.getMarketType());

                List<StockPrice> histories = stockPriceMap.getOrDefault(item.getIsinCode(), List.of());
//                StockStatResult stat = stockStatCalculator.calculate(stock, histories);

//                totalDefense += stat.defense() * weight;
//                totalAttack += stat.attack() * weight;
//                totalEndurance += stat.endurance() * weight;
//                totalAgility += stat.agility() * weight;
            } else if (item.getAssetType() == AssetType.CASH) {
                totalDefense += CashScorePolicy.DEFENSE.getScore() * weight;
                totalAttack += CashScorePolicy.ATTACK.getScore() * weight;
                totalEndurance += CashScorePolicy.ENDURANCE.getScore() * weight;
                totalAgility += CashScorePolicy.AGILITY.getScore() * weight;
            }
        }

        int balanceScore = BalanceScorePolicy.calculate(stockCount, marketTypes);

        Map<String, Integer> categories = new HashMap<>();
        categories.put(DiagnosisCategory.DEFENSE.getKey(), (int) Math.round(totalDefense));
        categories.put(DiagnosisCategory.ATTACK.getKey(), (int) Math.round(totalAttack));
        categories.put(DiagnosisCategory.ENDURANCE.getKey(), (int) Math.round(totalEndurance));
        categories.put(DiagnosisCategory.AGILITY.getKey(), (int) Math.round(totalAgility));
        categories.put(DiagnosisCategory.BALANCE.getKey(), balanceScore);

        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int overallScore = (int) Math.round(average);

        return new CalculatedHealth(overallScore, categories);
    }
}