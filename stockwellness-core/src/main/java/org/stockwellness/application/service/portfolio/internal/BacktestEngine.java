package org.stockwellness.application.service.portfolio.internal;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.portfolio.RebalancingPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BacktestEngine {

    public BacktestResult runLumpSum(SimulationData data, Map<String, BigDecimal> weights, BigDecimal initialAmount, RebalancingPeriod rebalancingPeriod) {
        // 모든 벤치마크 날짜의 합집합을 기준일로 설정 (합집합 정렬)
        List<LocalDate> allDates = data.benchmarkPrices().values().stream()
                .flatMap(List::stream)
                .map(StockPriceResult::baseDate)
                .distinct()
                .sorted()
                .toList();

        if (allDates.isEmpty()) return BacktestCalculator.calculate(Collections.emptyList());

        // 종목 및 지수 시세 배열 생성
        Map<String, BigDecimal[]> priceArrays = createAlignedPriceArrays(data.stockPrices(), allDates);
        Map<String, BigDecimal[]> benchmarkArrays = createAlignedPriceArrays(data.benchmarkPrices(), allDates);
        
        // 각 지수별 시작가 저장
        Map<String, BigDecimal> initialBenchmarkPrices = new HashMap<>();
        benchmarkArrays.forEach((ticker, prices) -> initialBenchmarkPrices.put(ticker, prices[0]));

        // 시작일 기준 수량 계산
        Map<String, BigDecimal> shares = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal allocatedAmount = initialAmount.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            BigDecimal price = priceArrays.getOrDefault(symbol, new BigDecimal[]{BigDecimal.ZERO})[0];
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                shares.put(symbol, allocatedAmount.divide(price, 8, RoundingMode.HALF_UP));
            } else {
                shares.put(symbol, BigDecimal.ZERO);
            }
        }

        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>(allDates.size());
        LocalDate lastRebalanceDate = allDates.get(0);

        for (int i = 0; i < allDates.size(); i++) {
            LocalDate date = allDates.get(i);
            
            // 리밸런싱 수행
            if (i > 0 && isRebalanceDay(date, lastRebalanceDate, rebalancingPeriod)) {
                BigDecimal dailyValue = calculateDailyValue(shares, priceArrays, i);
                for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
                    String symbol = entry.getKey();
                    BigDecimal targetValue = dailyValue.multiply(entry.getValue()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal currentPrice = priceArrays.getOrDefault(symbol, new BigDecimal[]{BigDecimal.ZERO})[i];
                    if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                        shares.put(symbol, targetValue.divide(currentPrice, 8, RoundingMode.HALF_UP));
                    }
                }
                lastRebalanceDate = date;
            }

            BigDecimal dailyValue = calculateDailyValue(shares, priceArrays, i);
            BigDecimal returnRate = calculateRate(dailyValue.subtract(initialAmount), initialAmount);
            
            // 다중 지수 수익률 계산
            Map<String, BigDecimal> benchmarkReturnRates = new HashMap<>();
            for (String ticker : benchmarkArrays.keySet()) {
                BigDecimal initialPrice = initialBenchmarkPrices.get(ticker);
                BigDecimal currentPrice = benchmarkArrays.get(ticker)[i];
                benchmarkReturnRates.put(ticker, calculateRate(currentPrice.subtract(initialPrice), initialPrice));
            }

            results.add(new BacktestResult.DailyBacktestResult(date, dailyValue, initialAmount, returnRate, benchmarkReturnRates));
        }

        return BacktestCalculator.calculate(results);
    }

    public BacktestResult runDCA(SimulationData data, Map<String, BigDecimal> weights, BigDecimal monthlyAmount, RebalancingPeriod rebalancingPeriod) {
        List<LocalDate> allDates = data.benchmarkPrices().values().stream()
                .flatMap(List::stream)
                .map(StockPriceResult::baseDate)
                .distinct()
                .sorted()
                .toList();

        if (allDates.isEmpty()) return BacktestCalculator.calculate(Collections.emptyList());

        Map<String, BigDecimal[]> priceArrays = createAlignedPriceArrays(data.stockPrices(), allDates);
        Map<String, BigDecimal[]> benchmarkArrays = createAlignedPriceArrays(data.benchmarkPrices(), allDates);
        
        Map<String, BigDecimal> initialBenchmarkPrices = new HashMap<>();
        benchmarkArrays.forEach((ticker, prices) -> initialBenchmarkPrices.put(ticker, prices[0]));

        Map<String, BigDecimal> totalShares = new HashMap<>();
        weights.keySet().forEach(s -> totalShares.put(s, BigDecimal.ZERO));
        
        BigDecimal totalInvested = BigDecimal.ZERO;
        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>(allDates.size());
        LocalDate lastInvestMonth = null;
        LocalDate lastRebalanceDate = allDates.get(0);

        for (int i = 0; i < allDates.size(); i++) {
            LocalDate date = allDates.get(i);
            
            if (lastInvestMonth == null || date.getMonthValue() != lastInvestMonth.getMonthValue()) {
                totalInvested = totalInvested.add(monthlyAmount);
                for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
                    String symbol = entry.getKey();
                    BigDecimal allocatedAmount = monthlyAmount.multiply(entry.getValue()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal price = priceArrays.getOrDefault(symbol, new BigDecimal[]{BigDecimal.ZERO})[i];
                    if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                        totalShares.put(symbol, totalShares.get(symbol).add(allocatedAmount.divide(price, 8, RoundingMode.HALF_UP)));
                    }
                }
                lastInvestMonth = date;
            }

            if (i > 0 && isRebalanceDay(date, lastRebalanceDate, rebalancingPeriod)) {
                BigDecimal dailyValue = calculateDailyValue(totalShares, priceArrays, i);
                for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
                    String symbol = entry.getKey();
                    BigDecimal targetValue = dailyValue.multiply(entry.getValue()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal currentPrice = priceArrays.getOrDefault(symbol, new BigDecimal[]{BigDecimal.ZERO})[i];
                    if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                        totalShares.put(symbol, targetValue.divide(currentPrice, 8, RoundingMode.HALF_UP));
                    }
                }
                lastRebalanceDate = date;
            }

            BigDecimal dailyValue = calculateDailyValue(totalShares, priceArrays, i);
            BigDecimal returnRate = calculateRate(dailyValue.subtract(totalInvested), totalInvested);
            
            Map<String, BigDecimal> benchmarkReturnRates = new HashMap<>();
            for (String ticker : benchmarkArrays.keySet()) {
                BigDecimal initialPrice = initialBenchmarkPrices.get(ticker);
                BigDecimal currentPrice = benchmarkArrays.get(ticker)[i];
                benchmarkReturnRates.put(ticker, calculateRate(currentPrice.subtract(initialPrice), initialPrice));
            }

            results.add(new BacktestResult.DailyBacktestResult(date, dailyValue, totalInvested, returnRate, benchmarkReturnRates));
        }

        return BacktestCalculator.calculate(results);
    }

    private BigDecimal calculateDailyValue(Map<String, BigDecimal> shares, Map<String, BigDecimal[]> priceArrays, int index) {
        BigDecimal value = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
            BigDecimal price = priceArrays.getOrDefault(entry.getKey(), new BigDecimal[]{BigDecimal.ZERO})[index];
            value = value.add(entry.getValue().multiply(price));
        }
        return value;
    }

    private boolean isRebalanceDay(LocalDate currentDate, LocalDate lastDate, RebalancingPeriod period) {
        if (period == null || period == RebalancingPeriod.NONE) return false;
        return switch (period) {
            case MONTHLY -> currentDate.getMonthValue() != lastDate.getMonthValue();
            case QUARTERLY -> (currentDate.getMonthValue() - 1) / 3 != (lastDate.getMonthValue() - 1) / 3;
            case YEARLY -> currentDate.getYear() != lastDate.getYear();
            default -> false;
        };
    }

    private Map<String, BigDecimal[]> createAlignedPriceArrays(Map<String, List<StockPriceResult>> sourceData, List<LocalDate> allDates) {
        Map<String, BigDecimal[]> alignedPrices = new HashMap<>();
        sourceData.forEach((symbol, prices) -> {
            NavigableMap<LocalDate, BigDecimal> priceMap = prices.stream()
                    .collect(Collectors.toMap(StockPriceResult::baseDate, StockPriceResult::closePrice, (v1, v2) -> v1, TreeMap::new));
            
            BigDecimal[] array = new BigDecimal[allDates.size()];
            BigDecimal lastPrice = BigDecimal.ZERO;
            for (int i = 0; i < allDates.size(); i++) {
                Map.Entry<LocalDate, BigDecimal> entry = priceMap.floorEntry(allDates.get(i));
                if (entry != null) {
                    lastPrice = entry.getValue();
                }
                array[i] = lastPrice;
            }
            alignedPrices.put(symbol, array);
        });
        return alignedPrices;
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }
}
