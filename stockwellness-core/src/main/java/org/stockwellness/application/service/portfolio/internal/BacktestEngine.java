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

        // 조회를 위한 Map 변환 (O(1) 조회용)
        Map<String, NavigableMap<LocalDate, BigDecimal>> priceLookup = createPriceLookup(data);
        NavigableMap<LocalDate, BigDecimal> benchmarkLookup = createBenchmarkLookup(data.benchmarkPrices());

        // 시작일 기준 수량 계산
        LocalDate startDate = allDates.get(0);
        Map<String, BigDecimal> shares = new HashMap<>();
        
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal allocatedAmount = initialAmount.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            BigDecimal price = getPriceAt(priceLookup.get(symbol), startDate);
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                shares.put(symbol, allocatedAmount.divide(price, 8, RoundingMode.HALF_UP));
            } else {
                shares.put(symbol, BigDecimal.ZERO);
            }
        }

        BigDecimal initialBenchmarkPrice = data.benchmarkPrices().get(0).closePrice();
        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>();

        for (LocalDate date : allDates) {
            BigDecimal dailyValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
                BigDecimal price = getPriceAt(priceLookup.get(entry.getKey()), date);
                dailyValue = dailyValue.add(entry.getValue().multiply(price));
            }

            BigDecimal returnRate = calculateRate(dailyValue.subtract(initialAmount), initialAmount);
            
            BigDecimal currentBenchmarkPrice = getPriceAt(benchmarkLookup, date);
            BigDecimal benchmarkReturnRate = calculateRate(currentBenchmarkPrice.subtract(initialBenchmarkPrice), initialBenchmarkPrice);

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

        Map<String, NavigableMap<LocalDate, BigDecimal>> priceLookup = createPriceLookup(data);
        NavigableMap<LocalDate, BigDecimal> benchmarkLookup = createBenchmarkLookup(data.benchmarkPrices());

        Map<String, BigDecimal> totalShares = new HashMap<>();
        weights.keySet().forEach(s -> totalShares.put(s, BigDecimal.ZERO));
        
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal initialBenchmarkPrice = data.benchmarkPrices().get(0).closePrice();
        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>();

        LocalDate lastInvestMonth = null;

        for (LocalDate date : allDates) {
            if (lastInvestMonth == null || date.getMonthValue() != lastInvestMonth.getMonthValue()) {
                totalInvested = totalInvested.add(monthlyAmount);
                for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
                    String symbol = entry.getKey();
                    BigDecimal weight = entry.getValue();
                    BigDecimal allocatedAmount = monthlyAmount.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    
                    BigDecimal price = getPriceAt(priceLookup.get(symbol), date);
                    if (price.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newShares = allocatedAmount.divide(price, 8, RoundingMode.HALF_UP);
                        totalShares.put(symbol, totalShares.get(symbol).add(newShares));
                    }
                }
                lastInvestMonth = date;
            }

            BigDecimal dailyValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : totalShares.entrySet()) {
                BigDecimal price = getPriceAt(priceLookup.get(entry.getKey()), date);
                dailyValue = dailyValue.add(entry.getValue().multiply(price));
            }

            BigDecimal returnRate = calculateRate(dailyValue.subtract(totalInvested), totalInvested);
            
            BigDecimal currentBenchmarkPrice = getPriceAt(benchmarkLookup, date);
            BigDecimal benchmarkReturnRate = calculateRate(currentBenchmarkPrice.subtract(initialBenchmarkPrice), initialBenchmarkPrice);

            results.add(new BacktestResult.DailyBacktestResult(date, dailyValue, totalInvested, returnRate, benchmarkReturnRate));
        }

        return new BacktestResult(results);
    }

    private Map<String, NavigableMap<LocalDate, BigDecimal>> createPriceLookup(SimulationData data) {
        Map<String, NavigableMap<LocalDate, BigDecimal>> lookup = new HashMap<>();
        data.stockPrices().forEach((symbol, prices) -> {
            TreeMap<LocalDate, BigDecimal> priceMap = prices.stream()
                    .collect(Collectors.toMap(
                            StockPriceResult::baseDate,
                            StockPriceResult::closePrice,
                            (v1, v2) -> v1,
                            TreeMap::new
                    ));
            lookup.put(symbol, priceMap);
        });
        return lookup;
    }

    private NavigableMap<LocalDate, BigDecimal> createBenchmarkLookup(List<StockPriceResult> benchmarkPrices) {
        return benchmarkPrices.stream()
                .collect(Collectors.toMap(
                        StockPriceResult::baseDate,
                        StockPriceResult::closePrice,
                        (v1, v2) -> v1,
                        TreeMap::new
                ));
    }

    private BigDecimal getPriceAt(NavigableMap<LocalDate, BigDecimal> priceMap, LocalDate date) {
        if (priceMap == null || priceMap.isEmpty()) return BigDecimal.ZERO;
        
        // 해당 날짜 가격이 있으면 즉시 반환, 없으면 가장 가까운 이전 날짜 가격 반환
        Map.Entry<LocalDate, BigDecimal> entry = priceMap.floorEntry(date);
        return (entry != null) ? entry.getValue() : BigDecimal.ZERO;
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
