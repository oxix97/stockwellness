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

/**
 * 포트폴리오의 건강 상태(오각형 지표)를 계산하는 핵심 컴포넌트입니다.
 * 수익성, 안정성, 분산성, 민첩성, 현금 비중의 5개 카테고리별로 0~100점 사이의 점수를 산출합니다.
 */
@Component
@RequiredArgsConstructor
public class PortfolioHealthCalculator {

    /**
     * 주어진 진단 컨텍스트를 바탕으로 포트폴리오의 종합 건강도와 상세 지표를 계산합니다.
     *
     * @param context 포트폴리오, 시세, 백테스트 결과, 상관계수 행렬 등을 포함한 진단 데이터
     * @return 계산된 종합 점수, 카테고리별 점수, 종목별 기여도 분석 결과
     */
    public CalculatedHealth calculate(DiagnosisContext context) {
        Portfolio portfolio = context.portfolio();
        Map<String, Stock> stockMap = context.stockMap();
        BacktestResult backtestResult = context.backtestResult();

        BigDecimal totalValue = portfolio.calculateTotalPurchaseAmount();
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new CalculatedHealth(0, Collections.emptyMap(), Collections.emptyList());
        }

        // 1. 기초 데이터 집계: 현금 비중, 종목 수, 시장 다양성 확인
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

        // [수익] - Alpha 기반 (벤치마크 대비 초과 수익률)
        // Alpha가 10% 이상이면 100점, 0%면 70점(시장 수준), -10% 이하면 40점 이하
        BigDecimal alpha = (backtestResult != null) ? backtestResult.alpha() : BigDecimal.ZERO;
        int returnScore = Math.min(100, Math.max(0, alpha.multiply(BigDecimal.valueOf(3.0)).add(BigDecimal.valueOf(70)).intValue()));
        categories.put(DiagnosisCategory.RETURN.getKey(), returnScore);

        // [안전] - Relative MDD 기반 (벤치마크 대비 추가 하락폭)
        // Relative MDD가 0 이하(시장보다 덜 하락)면 100점, 10%p 더 하락 시 70점, 20%p 더 하락 시 40점
        BigDecimal relativeMdd = (backtestResult != null) ? backtestResult.relativeMdd() : BigDecimal.ZERO;
        int stabilityScore = Math.max(0, 100 - relativeMdd.multiply(BigDecimal.valueOf(3.0)).intValue());
        categories.put(DiagnosisCategory.STABILITY.getKey(), stabilityScore);

        // [분산(DIVERSIFICATION)] - HHI 지수, 시장 다양성, 상관계수 기반
        // HHI(Herfindahl-Hirschman Index) = sum(각 종목 비중^2). 낮을수록 분산이 잘 된 것.
        double hhi = assetWeights.values().stream().mapToDouble(w -> Math.pow(w.doubleValue(), 2)).sum();
        int divScoreFromHhi = Math.max(0, 100 - (int) (hhi * 80));
        
        // 코스피/코스닥/나스닥 등 시장이 다양할수록 보너스 (최대 20점)
        int marketBonus = Math.min(20, marketTypes.size() * 7);

        // 종목 간 상관관계 분석: 평균 상관계수가 낮을수록 분산 효과가 크므로 보너스 부여
        int correlationAdjustment = calculateCorrelationAdjustment(context.correlationMatrix());

        int diversificationScore = Math.min(100, divScoreFromHhi + marketBonus + (Math.min(5, stockCount) * 4) + correlationAdjustment);
        categories.put(DiagnosisCategory.DIVERSIFICATION.getKey(), diversificationScore);

        // [민첩(AGILITY)] - 현금 비중의 적절성 기반 (하락 시 대응 능력)
        // 10~25% 사이가 가장 민첩한 상태(100점), 0%면 40점, 너무 높으면 기회비용 발생으로 감점
        int agilityScore;
        if (cashWeight.compareTo(BigDecimal.valueOf(10)) >= 0 && cashWeight.compareTo(BigDecimal.valueOf(25)) <= 0) {
            agilityScore = 100;
        } else if (cashWeight.compareTo(BigDecimal.valueOf(10)) < 0) {
            agilityScore = cashWeight.multiply(BigDecimal.valueOf(6)).add(BigDecimal.valueOf(40)).intValue();
        } else {
            agilityScore = Math.max(50, 100 - cashWeight.subtract(BigDecimal.valueOf(25)).multiply(BigDecimal.valueOf(1.5)).intValue());
        }
        categories.put(DiagnosisCategory.AGILITY.getKey(), agilityScore);

        // [현금(CASH)] - 절대적 현금 비중 수치
        // 30% 이상이면 현금 보유량 100점, 그 이하는 비례하여 산출
        int cashScore = Math.min(100, cashWeight.multiply(BigDecimal.valueOf(3.33)).intValue());
        categories.put(DiagnosisCategory.CASH.getKey(), cashScore);

        // 3. 종목별 기여도 산출: 포트폴리오 수익률에 어떤 종목이 얼마나 기여했는지 분석
        List<StockContributionResult> stockContributions = new ArrayList<>();
        if (backtestResult != null && backtestResult.itemReturns() != null) {
            Map<String, BigDecimal> itemReturns = backtestResult.itemReturns();
            
            // 수익률 기준 정렬 (수익률이 높은 종목이 먼저 오도록)
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

        // 4. 종합 점수 산출 (5개 지표의 평균)
        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new CalculatedHealth((int) Math.round(average), categories, stockContributions);
    }

    /**
     * 종목 간 상관계수 평균을 바탕으로 분산 점수를 조정합니다.
     * 상관계수가 낮을수록(종목들이 따로 움직일수록) 분산 효과가 좋으므로 가점을 부여합니다.
     */
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
        // 0.2 미만: 매우 좋은 분산 (+10점)
        // 0.6 이상: 종목들이 너무 비슷하게 움직임 (-5점 ~ -10점)
        if (avgCorr < 0.2) return 10;
        if (avgCorr < 0.4) return 5;
        if (avgCorr > 0.7) return -10;
        if (avgCorr > 0.6) return -5;
        return 0;
    }
}
