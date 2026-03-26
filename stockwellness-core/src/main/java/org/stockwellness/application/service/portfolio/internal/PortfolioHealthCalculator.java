package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PortfolioHealthCalculator {

    public CalculatedHealth calculate(DiagnosisContext context) {
        Portfolio portfolio = context.portfolio();
        Map<String, Stock> stockMap = context.stockMap();
        BacktestResult backtestResult = context.backtestResult();

        BigDecimal totalValue = portfolio.calculateTotalPurchaseAmount();
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new CalculatedHealth(0, Collections.emptyMap());
        }

        // 1. 기초 데이터 집계
        BigDecimal cashValue = BigDecimal.ZERO;
        int stockCount = 0;
        Set<MarketType> marketTypes = new HashSet<>();
        Map<String, BigDecimal> assetWeights = new HashMap<>();

        for (PortfolioItem item : portfolio.getItems()) {
            BigDecimal itemValue = item.calculatePurchaseAmount();
            if (item.getAssetType() == AssetType.CASH) {
                cashValue = cashValue.add(itemValue);
            } else {
                stockCount++;
                Stock stock = stockMap.get(item.getSymbol());
                if (stock != null) {
                    marketTypes.add(stock.getMarketType());
                }
                assetWeights.put(item.getSymbol(), itemValue.divide(totalValue, 4, RoundingMode.HALF_UP));
            }
        }

        BigDecimal cashWeight = cashValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // 2. 카테고리별 점수 산출 (0~100점)
        Map<String, Integer> categories = new HashMap<>();

        // [수익] - CAGR 기반 (20% 이상 100점, 0% 50점, -20% 0점)
        BigDecimal cagr = (backtestResult != null) ? backtestResult.cagr() : BigDecimal.valueOf(5); // 기본값 5%
        int returnScore = Math.min(100, Math.max(0, cagr.multiply(BigDecimal.valueOf(2.5)).add(BigDecimal.valueOf(50)).intValue()));
        categories.put(DiagnosisCategory.RETURN.getKey(), returnScore);

        // [안전] - MDD 기반 (0% 100점, 20% 50점, 40% 0점)
        BigDecimal mdd = (backtestResult != null) ? backtestResult.mdd() : BigDecimal.valueOf(15); // 기본값 15%
        int stabilityScore = Math.max(0, 100 - mdd.multiply(BigDecimal.valueOf(2.5)).intValue());
        categories.put(DiagnosisCategory.STABILITY.getKey(), stabilityScore);

        // [분산] - HHI(Herfindahl-Hirschman Index) 역산 + 시장 다양성
        // HHI = sum(w^2). HHI가 낮을수록 분산이 잘 된 것. (완전분산 0.1, 집중투자 1.0)
        double hhi = assetWeights.values().stream().mapToDouble(w -> Math.pow(w.doubleValue(), 2)).sum();
        int divScoreFromHhi = Math.max(0, 100 - (int)(hhi * 80)); 
        int marketBonus = Math.min(20, marketTypes.size() * 7);
        int diversificationScore = Math.min(100, divScoreFromHhi + marketBonus + (Math.min(5, stockCount) * 4));
        categories.put(DiagnosisCategory.DIVERSIFICATION.getKey(), diversificationScore);

        // [민첩] - 현금 비중 기반 (10~25% 사이가 최적 100점, 0%면 40점, 50% 이상이면 60점)
        int agilityScore;
        if (cashWeight.compareTo(BigDecimal.valueOf(10)) >= 0 && cashWeight.compareTo(BigDecimal.valueOf(25)) <= 0) {
            agilityScore = 100;
        } else if (cashWeight.compareTo(BigDecimal.valueOf(10)) < 0) {
            agilityScore = cashWeight.multiply(BigDecimal.valueOf(6)).add(BigDecimal.valueOf(40)).intValue();
        } else {
            agilityScore = Math.max(50, 100 - cashWeight.subtract(BigDecimal.valueOf(25)).multiply(BigDecimal.valueOf(1.5)).intValue());
        }
        categories.put(DiagnosisCategory.AGILITY.getKey(), agilityScore);

        // [현금] - 절대적 현금 비중 (30% 이상이면 100점, 그 이하는 비례)
        int cashScore = Math.min(100, cashWeight.multiply(BigDecimal.valueOf(3.33)).intValue());
        categories.put(DiagnosisCategory.CASH.getKey(), cashScore);

        // 3. 종합 점수 산출
        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new CalculatedHealth((int) Math.round(average), categories);
    }
}
