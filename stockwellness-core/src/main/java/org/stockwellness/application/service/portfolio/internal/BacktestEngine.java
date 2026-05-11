package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.portfolio.RebalancingPeriod;
import org.stockwellness.domain.portfolio.indicator.BacktestAggregator;
import org.stockwellness.domain.portfolio.indicator.IndicatorCalculator;
import org.stockwellness.domain.portfolio.math.FinancialMath;
import org.stockwellness.domain.portfolio.strategy.CashFlowModel;
import org.stockwellness.domain.portfolio.strategy.DCAModel;
import org.stockwellness.domain.portfolio.strategy.LumpSumModel;
import org.stockwellness.domain.portfolio.strategy.RebalancingStrategy;
import org.stockwellness.domain.portfolio.vo.ReturnSeries;

/**
 * 포트폴리오의 과거 수익률을 시뮬레이션하는 백테스트 엔진입니다.
 * 거치식(Lump Sum) 및 적립식(DCA) 투자 전략과 다양한 리밸런싱 주기를 지원합니다.
 */
@Component
public class BacktestEngine {

    private final RebalancingStrategy rebalancingStrategy = new RebalancingStrategy();
    private final BacktestAggregator aggregator = new BacktestAggregator();

    /**
     * 거치식(Lump Sum) 백테스트를 실행합니다.
     * 초기 투자금을 시작일에 모두 투자하고, 설정된 리밸런싱 주기에 따라 비중을 재조정합니다.
     *
     * @param data 시뮬레이션에 필요한 종목 및 지수 시세 데이터
     * @param weights 종목별 투자 비중 (%)
     * @param initialAmount 초기 투자 금액
     * @param rebalancingPeriod 리밸런싱 주기 (없음, 매월, 매분기, 매년)
     * @param primaryBenchmarkTicker 비교 기준이 되는 지수 티커
     * @param riskFreeRate 무위험 수익률 (샤프 지수 계산용)
     * @return 일별 수익률 및 최종 통계가 포함된 백테스트 결과
     */
    public BacktestResult runLumpSum(SimulationData data, Map<String, BigDecimal> weights, BigDecimal initialAmount, RebalancingPeriod rebalancingPeriod, String primaryBenchmarkTicker, BigDecimal riskFreeRate, boolean dividendReinvested) {
        return runSimulation(data, weights, new LumpSumModel(initialAmount), rebalancingPeriod, primaryBenchmarkTicker, riskFreeRate, dividendReinvested);
    }

    /**
     * 적립식(DCA, Dollar Cost Averaging) 백테스트를 실행합니다.
     * 매월 정해진 금액을 추가 투자하며 포트폴리오를 관리합니다.
     *
     * @param data 시뮬레이션에 필요한 시세 데이터
     * @param weights 종목별 투자 비중 (%)
     * @param monthlyAmount 매월 투자할 금액
     * @param rebalancingPeriod 리밸런싱 주기
     * @param primaryBenchmarkTicker 비교 기준 지수 티커
     * @param riskFreeRate 무위험 수익률
     * @param dividendReinvested 배당금 재투자 여부
     * @return 백테스트 결과
     */
    public BacktestResult runDCA(SimulationData data, Map<String, BigDecimal> weights, BigDecimal monthlyAmount, RebalancingPeriod rebalancingPeriod, String primaryBenchmarkTicker, BigDecimal riskFreeRate, boolean dividendReinvested) {
        return runSimulation(data, weights, new DCAModel(monthlyAmount), rebalancingPeriod, primaryBenchmarkTicker, riskFreeRate, dividendReinvested);
    }

    /**
     * 공통 시뮬레이션 실행 로직
     */
    private BacktestResult runSimulation(SimulationData data, Map<String, BigDecimal> weights, CashFlowModel cashFlowModel, RebalancingPeriod rebalancingPeriod, String primaryBenchmarkTicker, BigDecimal riskFreeRate, boolean dividendReinvested) {
        // 1. 모든 데이터(종목, 지수)의 날짜를 합쳐서 전체 시뮬레이션 기간의 기준일 리스트 생성
        List<LocalDate> allDates = extractSortedDates(data);
        if (allDates.isEmpty()) return BacktestResult.empty();

        // 2. 계산 효율을 위해 시세 데이터를 날짜 순으로 정렬된 배열로 변환
        Map<String, BigDecimal[]> priceArrays = createAlignedPriceArrays(data.stockPrices(), allDates, dividendReinvested);
        Map<String, BigDecimal[]> benchmarkArrays = createAlignedPriceArrays(data.benchmarkPrices(), allDates, false); // 지수는 일반 종가 사용

        List<BacktestResult.DailyBacktestResult> results = new ArrayList<>();
        Map<LocalDate, BigDecimal> dailyPortfolioReturns = new TreeMap<>();
        List<BigDecimal> dailyValues = new ArrayList<>();
        Map<String, BigDecimal> totalShares = new HashMap<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal previousValue = BigDecimal.ZERO;
        LocalDate lastRebalanceDate = allDates.getFirst();

        // 3. 일별 시뮬레이션 루프
        for (int i = 0; i < allDates.size(); i++) {
            LocalDate date = allDates.get(i);
            Map<String, BigDecimal> currentPrices = extractCurrentPrices(priceArrays, i);

            // 1) 입금 및 초기 매수 (Cash Flow)
            BigDecimal deposit = BigDecimal.ZERO;
            if (i == 0) {
                deposit = cashFlowModel.getInitialAmount();
            } else if (cashFlowModel instanceof DCAModel && date.getMonthValue() != allDates.get(i - 1).getMonthValue()) {
                deposit = ((DCAModel) cashFlowModel).monthlyAmount();
            }

            if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                totalInvested = totalInvested.add(deposit);
                invest(totalShares, deposit, weights, currentPrices);
            }

            // 2) 리밸런싱 수행: 설정된 주기가 도래하면 현재 가치 기준으로 수량 재계산
            BigDecimal currentValueBeforeRebalance = calculateDailyValue(totalShares, priceArrays, i);
            if (rebalancingStrategy.shouldRebalance(date, lastRebalanceDate, rebalancingPeriod)) {
                rebalancingStrategy.rebalance(totalShares, currentValueBeforeRebalance, weights, currentPrices);
                lastRebalanceDate = date;
            }

            // 3) 일일 가치 및 수익률 계산
            BigDecimal dailyValue = calculateDailyValue(totalShares, priceArrays, i);
            dailyValues.add(dailyValue);

            BigDecimal returnRate = FinancialMath.calculateReturnRate(totalInvested, dailyValue);
            BigDecimal dailyReturn = i == 0 ? BigDecimal.ZERO : FinancialMath.calculateReturnRate(previousValue, dailyValue);
            if (i > 0) dailyPortfolioReturns.put(date, dailyReturn);

            results.add(new BacktestResult.DailyBacktestResult(
                date, dailyValue, totalInvested, returnRate,
                calculateBenchmarkReturns(benchmarkArrays, i, 0)
            ));

            previousValue = dailyValue;
        }

        // 4. 최종 결과 집계 및 통계 산출
        return aggregateResults(
            results, dailyPortfolioReturns, dailyValues, totalInvested, previousValue, 
            allDates.size(), benchmarkArrays, primaryBenchmarkTicker, null, 
            calculateItemReturns(weights, totalShares, priceArrays, allDates.size() - 1, totalInvested), 
            riskFreeRate
        );
    }

    /**
     * 시뮬레이션 기간의 모든 기준일(Date)을 추출하여 정렬합니다.
     */
    private List<LocalDate> extractSortedDates(SimulationData data) {
        return data.benchmarkPrices().values().stream()
                .flatMap(List::stream)
                .map(StockPriceResult::baseDate)
                .distinct()
                .sorted()
                .toList();
    }

    private void invest(Map<String, BigDecimal> totalShares, BigDecimal amount, Map<String, BigDecimal> weights, Map<String, BigDecimal> currentPrices) {
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal allocated = amount.multiply(weight).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            BigDecimal price = currentPrices.getOrDefault(symbol, BigDecimal.ZERO);
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal shares = allocated.divide(price, 8, RoundingMode.HALF_UP);
                totalShares.put(symbol, totalShares.getOrDefault(symbol, BigDecimal.ZERO).add(shares));
            }
        }
    }

    private Map<String, BigDecimal> extractCurrentPrices(Map<String, BigDecimal[]> priceArrays, int index) {
        Map<String, BigDecimal> prices = new HashMap<>();
        priceArrays.forEach((symbol, array) -> prices.put(symbol, array[index]));
        return prices;
    }

    private Map<String, BigDecimal> calculateBenchmarkReturns(Map<String, BigDecimal[]> benchmarkArrays, int currentIndex, int startIndex) {
        Map<String, BigDecimal> returns = new HashMap<>();
        benchmarkArrays.forEach((ticker, array) -> {
            BigDecimal startPrice = array[startIndex];
            BigDecimal currentPrice = array[currentIndex];
            returns.put(ticker, FinancialMath.calculateReturnRate(startPrice, currentPrice));
        });
        return returns;
    }

    private Map<String, BigDecimal> calculateItemReturns(Map<String, BigDecimal> weights, Map<String, BigDecimal> shares, Map<String, BigDecimal[]> priceArrays, int lastIdx, BigDecimal totalInvested) {
        Map<String, BigDecimal> itemReturns = new HashMap<>();
        for (String symbol : weights.keySet()) {
            BigDecimal share = shares.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal currentPrice = priceArrays.getOrDefault(symbol, new BigDecimal[]{BigDecimal.ZERO})[lastIdx];
            BigDecimal currentValue = share.multiply(currentPrice);
            BigDecimal initialAllocated = totalInvested.multiply(weights.get(symbol)).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            
            // 기여도 = (해당 종목의 현재 가치 - 초기 할당금액) / 초기 전체 투자금 * 100
            BigDecimal profitLoss = currentValue.subtract(initialAllocated);
            itemReturns.put(symbol, FinancialMath.calculateReturnRate(totalInvested, profitLoss.add(totalInvested))); // Using calculateReturnRate for consistency
            // Wait, profitLoss / totalInvested is the contribution.
            itemReturns.put(symbol, profitLoss.divide(totalInvested, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP));
        }
        return itemReturns;
    }

    private BacktestResult aggregateResults(
            List<BacktestResult.DailyBacktestResult> dailyResults,
            Map<LocalDate, BigDecimal> dailyReturns,
            List<BigDecimal> dailyValues,
            BigDecimal initialAmount,
            BigDecimal finalAmount,
            int totalDays,
            Map<String, BigDecimal[]> benchmarkArrays,
            String primaryBenchmarkTicker,
            String aiComment,
            Map<String, BigDecimal> itemReturns,
            BigDecimal riskFreeRate
    ) {
        Map<String, ReturnSeries> benchmarkSeries = new HashMap<>();
        benchmarkArrays.forEach((ticker, array) -> {
            Map<LocalDate, BigDecimal> bReturns = new TreeMap<>();
            for (int i = 1; i < dailyResults.size(); i++) {
                bReturns.put(dailyResults.get(i).date(), FinancialMath.calculateReturnRate(array[i - 1], array[i]));
            }
            benchmarkSeries.put(ticker, new ReturnSeries(bReturns));
        });

        double years = totalDays / 252.0;
        BigDecimal portfolioCagr = FinancialMath.calculateCAGR(initialAmount, finalAmount, years);

        IndicatorCalculator.IndicatorContext context = new IndicatorCalculator.IndicatorContext(
                new ReturnSeries(dailyReturns),
                dailyValues,
                initialAmount,
                finalAmount,
                years,
                benchmarkSeries,
                primaryBenchmarkTicker,
                riskFreeRate,
                portfolioCagr
        );

        return aggregator.aggregate(dailyResults, context, itemReturns, aiComment);
    }

    /**
     * 특정 시점의 포트폴리오 총 가치를 계산합니다.
     */
    private BigDecimal calculateDailyValue(Map<String, BigDecimal> shares, Map<String, BigDecimal[]> priceArrays, int index) {
        BigDecimal value = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
            BigDecimal price = priceArrays.getOrDefault(entry.getKey(), new BigDecimal[]{BigDecimal.ZERO})[index];
            value = value.add(entry.getValue().multiply(price));
        }
        return value;
    }

    /**
     * 원본 시세 데이터를 전체 시뮬레이션 날짜에 맞춰 정렬된 배열로 변환합니다.
     * 중간에 빠진 날짜(휴장일 등)가 있다면 직전 가격을 채워 넣습니다.
     */
    private Map<String, BigDecimal[]> createAlignedPriceArrays(Map<String, List<StockPriceResult>> sourceData, List<LocalDate> allDates, boolean dividendReinvested) {
        Map<String, BigDecimal[]> alignedPrices = new HashMap<>();
        sourceData.forEach((symbol, prices) -> {
            NavigableMap<LocalDate, BigDecimal> priceMap = prices.stream()
                    .collect(Collectors.toMap(
                            StockPriceResult::baseDate, 
                            p -> dividendReinvested ? p.adjClosePrice() : p.closePrice(), 
                            (v1, v2) -> v1, 
                            TreeMap::new
                    ));
            
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
}
