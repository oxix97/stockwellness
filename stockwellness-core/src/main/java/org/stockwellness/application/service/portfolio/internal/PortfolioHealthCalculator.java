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

        BigDecimal totalValue = portfolio.calculateTotalPurchaseAmount();
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new CalculatedHealth(0, Collections.emptyMap());
        }

        // 1. 기초 데이터 집계
        BigDecimal cashValue = BigDecimal.ZERO;
        int stockCount = 0;
        Set<MarketType> marketTypes = new HashSet<>();
        BigDecimal totalProfitRate = BigDecimal.ZERO;

        for (PortfolioItem item : portfolio.getItems()) {
            if (item.getAssetType() == AssetType.CASH) {
                cashValue = cashValue.add(item.calculatePurchaseAmount());
            } else {
                stockCount++;
                Stock stock = stockMap.get(item.getSymbol());
                if (stock != null) {
                    marketTypes.add(stock.getMarketType());
                }
                // 임시: 수익률은 현재 시세 연동 전이므로 0으로 가정하거나 평단가 대비 계산 가능 시점까지 보류
            }
        }

        BigDecimal cashWeight = cashValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // 2. 카테고리별 점수 산출 (0~100점)
        Map<String, Integer> categories = new HashMap<>();
        
        // [수익] - 임시 구현 (실제 수익률 기반)
        categories.put(DiagnosisCategory.RETURN.getKey(), 75); 

        // [안전] - 현금 비중이 높을수록 가산점 (MDD 대용)
        int stabilityScore = Math.min(100, cashWeight.multiply(BigDecimal.valueOf(2)).intValue() + 40);
        categories.put(DiagnosisCategory.STABILITY.getKey(), stabilityScore);

        // [분산] - 종목 수 및 시장 다양성
        int divScore = Math.min(100, (stockCount * 10) + (marketTypes.size() * 15));
        categories.put(DiagnosisCategory.DIVERSIFICATION.getKey(), divScore);

        // [민첩] - 대응력 (현금 비중 10~30% 사이일 때 고득점)
        int agilityScore = (cashWeight.compareTo(BigDecimal.valueOf(10)) >= 0 && cashWeight.compareTo(BigDecimal.valueOf(30)) <= 0) ? 90 : 60;
        categories.put(DiagnosisCategory.AGILITY.getKey(), agilityScore);

        // [현금] - 절대적 현금 비중 점수화
        int cashScore = Math.min(100, cashWeight.multiply(BigDecimal.valueOf(3)).intValue());
        categories.put(DiagnosisCategory.CASH.getKey(), cashScore);

        // 3. 종합 점수 산출
        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new CalculatedHealth((int) Math.round(average), categories);
    }
}
