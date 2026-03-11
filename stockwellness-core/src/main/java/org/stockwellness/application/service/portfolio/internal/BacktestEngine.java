package org.stockwellness.application.service.portfolio.internal;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BacktestEngine {

    public BacktestResult runLumpSum(SimulationData data, Map<String, BigDecimal> weights, BigDecimal initialAmount) {
        List<LocalDate> allDates = data.benchmarkPrices().stream()
                .map(StockPriceResult::baseDate)
                .sorted()
                .toList();

        if (allDates.isEmpty()) return new BacktestResult(List.of());

        // 최적화: 시계열 데이터를 배열로 사전 정렬 및 매핑 (O(1) 접근)
        Map<String, BigDecimal[]> priceArrays = createAlignedPriceArrays(data, allDates);
        BigDecimal[] benchmarkArray = extractClosePrices(data.benchmarkPrices(), allDates);

        // 시작일 기준 수량 계산
        Map<String, BigDecimal> shares = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal allocatedAmount = initialAmount.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            BigDecimal price = priceArrays.get(symbol)[0];
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                shares.put(symbol, allocatedAmount.divide(price, 8, RoundingMode.HALF_UP));
            } else {
                shares.put(symbol, BigDecimal.ZERO);
            }
        }

        BigDecimal initialBenchmarkPrice = benchmarkArray[0];
        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>(allDates.size());

        for (int i = 0; i < allDates.size(); i++) {
            LocalDate date = allDates.get(i);
            BigDecimal dailyValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
                BigDecimal price = priceArrays.get(entry.getKey())[i];
                dailyValue = dailyValue.add(entry.getValue().multiply(price));
            }

            BigDecimal returnRate = calculateRate(dailyValue.subtract(initialAmount), initialAmount);
            BigDecimal benchmarkReturnRate = calculateRate(benchmarkArray[i].subtract(initialBenchmarkPrice), initialBenchmarkPrice);

            results.add(new BacktestResult.DailyBacktestResult(date, dailyValue, initialAmount, returnRate, benchmarkReturnRate));
        }

        return new BacktestResult(results);
    }

    public BacktestResult runDCA(SimulationData data, Map<String, BigDecimal> weights, BigDecimal monthlyAmount) {
        List<LocalDate> allDates = data.benchmarkPrices().stream()
                .map(StockPriceResult::baseDate)
                .sorted()
                .toList();

        if (allDates.isEmpty()) return new BacktestResult(List.of());

        Map<String, BigDecimal[]> priceArrays = createAlignedPriceArrays(data, allDates);
        BigDecimal[] benchmarkArray = extractClosePrices(data.benchmarkPrices(), allDates);

        Map<String, BigDecimal> totalShares = new HashMap<>();
        weights.keySet().forEach(s -> totalShares.put(s, BigDecimal.ZERO));
        
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal initialBenchmarkPrice = benchmarkArray[0];
        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>(allDates.size());

        LocalDate lastInvestMonth = null;

        for (int i = 0; i < allDates.size(); i++) {
            LocalDate date = allDates.get(i);
            
            // 매월 첫 거래일에 투자
            if (lastInvestMonth == null || date.getMonthValue() != lastInvestMonth.getMonthValue()) {
                totalInvested = totalInvested.add(monthlyAmount);
                for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
                    String symbol = entry.getKey();
                    BigDecimal weight = entry.getValue();
                    BigDecimal allocatedAmount = monthlyAmount.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    
                    BigDecimal price = priceArrays.get(symbol)[i];
                    if (price.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newShares = allocatedAmount.divide(price, 8, RoundingMode.HALF_UP);
                        totalShares.put(symbol, totalShares.get(symbol).add(newShares));
                    }
                }
                lastInvestMonth = date;
            }

            BigDecimal dailyValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : totalShares.entrySet()) {
                BigDecimal price = priceArrays.get(entry.getKey())[i];
                dailyValue = dailyValue.add(entry.getValue().multiply(price));
            }

            BigDecimal returnRate = calculateRate(dailyValue.subtract(totalInvested), totalInvested);
            BigDecimal benchmarkReturnRate = calculateRate(benchmarkArray[i].subtract(initialBenchmarkPrice), initialBenchmarkPrice);

            results.add(new BacktestResult.DailyBacktestResult(date, dailyValue, totalInvested, returnRate, benchmarkReturnRate));
        }

        return new BacktestResult(results);
    }

    /**
     * 모든 종목의 시세를 벤치마크 날짜 리스트에 맞춰 정렬된 배열로 변환합니다. (중간에 빈 날짜는 이전 날짜 가격으로 채움)
     */
    private Map<String, BigDecimal[]> createAlignedPriceArrays(SimulationData data, List<LocalDate> allDates) {
        Map<String, BigDecimal[]> alignedPrices = new HashMap<>();
        data.stockPrices().forEach((symbol, prices) -> {
            NavigableMap<LocalDate, BigDecimal> priceMap = prices.stream()
                    .collect(Collectors.toMap(StockPriceResult::baseDate, StockPriceResult::closePrice, (v1, v2) -> v1, TreeMap::new));
            
            BigDecimal[] array = new BigDecimal[allDates.size()];
            for (int i = 0; i < allDates.size(); i++) {
                Map.Entry<LocalDate, BigDecimal> entry = priceMap.floorEntry(allDates.get(i));
                array[i] = (entry != null) ? entry.getValue() : BigDecimal.ZERO;
            }
            alignedPrices.put(symbol, array);
        });
        return alignedPrices;
    }

    private BigDecimal[] extractClosePrices(List<StockPriceResult> prices, List<LocalDate> allDates) {
        NavigableMap<LocalDate, BigDecimal> priceMap = prices.stream()
                .collect(Collectors.toMap(StockPriceResult::baseDate, StockPriceResult::closePrice, (v1, v2) -> v1, TreeMap::new));
        
        BigDecimal[] array = new BigDecimal[allDates.size()];
        for (int i = 0; i < allDates.size(); i++) {
            Map.Entry<LocalDate, BigDecimal> entry = priceMap.floorEntry(allDates.get(i));
            array[i] = (entry != null) ? entry.getValue() : BigDecimal.ZERO;
        }
        return array;
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
