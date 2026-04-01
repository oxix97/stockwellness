package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.result.StockContributionResult;
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
            return new CalculatedHealth(0, Collections.emptyMap(), Collections.emptyList());
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

        // [분산] - HHI(Herfindahl-Hirschman Index) 역산 + 시장 다양성 + 상관관계 분석
        // HHI = sum(w^2). HHI가 낮을수록 분산이 잘 된 것. (완전분산 0.1, 집중투자 1.0)
        double hhi = assetWeights.values().stream().mapToDouble(w -> Math.pow(w.doubleValue(), 2)).sum();
        int divScoreFromHhi = Math.max(0, 100 - (int) (hhi * 80));
        int marketBonus = Math.min(20, marketTypes.size() * 7);

        // 상관관계 보너스/패널티 산출
        int correlationAdjustment = calculateCorrelationAdjustment(context.correlationMatrix());

        int diversificationScore = Math.min(100, divScoreFromHhi + marketBonus + (Math.min(5, stockCount) * 4) + correlationAdjustment);
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

        // 3. 종목별 기여도 산출
        List<StockContributionResult> stockContributions = new ArrayList<>();
        if (backtestResult != null && backtestResult.itemReturns() != null) {
            Map<String, BigDecimal> itemReturns = backtestResult.itemReturns();
            
            // 수익률 기준 정렬 (내림차순)
            List<Map.Entry<String, BigDecimal>> sortedReturns = itemReturns.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .toList();

            for (int i = 0; i < sortedReturns.size(); i++) {
                Map.Entry<String, BigDecimal> entry = sortedReturns.get(i);
                String symbol = entry.getKey();
                BigDecimal returnValue = entry.getValue();
                Stock stock = stockMap.get(symbol);
                String name = (stock != null) ? stock.getName() : symbol;

                String mainContribution;
                int score;
                String reason;

                if (returnValue.compareTo(BigDecimal.ZERO) > 0) {
                    if (i < 3) {
                        mainContribution = "주요 수익원";
                        score = Math.min(100, 80 + returnValue.intValue());
                        reason = String.format("포트폴리오 수익률에 %s%% 기여하며 성장을 견인하고 있습니다.", returnValue);
                    } else {
                        mainContribution = "보조 수익원";
                        score = Math.min(90, 70 + returnValue.intValue());
                        reason = "안정적인 수익을 유지하며 포트폴리오를 지지하고 있습니다.";
                    }
                } else {
                    mainContribution = "수익성 개선 필요";
                    score = Math.max(0, 50 + returnValue.intValue());
                    reason = String.format("최근 수익률이 %s%%로 저조하여 비중 조절이나 정밀 진단이 필요합니다.", returnValue);
                }

                stockContributions.add(new StockContributionResult(name, mainContribution, score, reason));
            }
        }

        // 4. 종합 점수 산출
        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new CalculatedHealth((int) Math.round(average), categories, stockContributions);
    }

    private int calculateCorrelationAdjustment(Map<String, Map<String, BigDecimal>> matrix) {
        if (matrix == null || matrix.size() < 2) return 0;

        double sum = 0;
        int count = 0;
        for (String s1 : matrix.keySet()) {
            for (String s2 : matrix.get(s1).keySet()) {
                if (!s1.equals(s2)) {
                    sum += matrix.get(s1).get(s2).doubleValue();
                    count++;
                }
            }
        }

        if (count == 0) return 0;
        double avgCorr = sum / count;

        // 평균 상관계수 기반 점수 조정 (-10 ~ +10점)
        if (avgCorr < 0.2) return 10;
        if (avgCorr < 0.4) return 5;
        if (avgCorr > 0.7) return -10;
        if (avgCorr > 0.6) return -5;
        return 0;
    }
}
