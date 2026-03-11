package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.PortfolioAnalysisUseCase;
import org.stockwellness.application.port.in.portfolio.command.BacktestPortfolioCommand;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.port.in.portfolio.result.*;
import org.stockwellness.application.service.portfolio.internal.*;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.BacktestStrategy;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.Country;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.FinanceCalculationUtil;
import org.stockwellness.global.util.PortfolioMapperUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 분석 서비스
 * 가치 평가, 분산도 분석, 리밸런싱 가이드, 백테스팅 및 상관관계 분석 로직을 통합 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisService implements PortfolioAnalysisUseCase {

    private final PortfolioAnalysisDataLoader dataLoader;
    private final SimulationDataProvider simulationDataProvider;
    private final StockPricePort stockPricePort;
    private final BacktestEngine backtestEngine;
    private final PortfolioCorrelationCalculator correlationCalculator;

    /**
     * 포트폴리오의 실시간 평가 가치 및 수익률을 조회합니다.
     */
    @Override
    public PortfolioValuationResult getValuation(Long memberId, Long portfolioId) {
        return calculateValuation(dataLoader.loadContext(portfolioId, memberId));
    }

    /**
     * 포트폴리오의 자산군, 업종, 국가별 분산 비중을 조회합니다.
     */
    @Override
    public PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId) {
        return calculateDiversification(dataLoader.loadContext(portfolioId, memberId));
    }

    /**
     * 목표 비중 대비 현재 상태를 비교하여 리밸런싱 매매 가이드를 조회합니다.
     */
    @Override
    public PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId) {
        return calculateRebalancing(dataLoader.loadContext(portfolioId, memberId));
    }

    /**
     * 포트폴리오 분석의 핵심 지표(가치, 분산, 리밸런싱)를 통합 요약하여 조회합니다.
     * 단일 Context 로딩을 통해 성능을 최적화합니다.
     */
    @Override
    public PortfolioAnalysisSummaryResult getAnalysisSummary(Long memberId, Long portfolioId) {
        AnalysisContext context = dataLoader.loadContext(portfolioId, memberId);
        
        return new PortfolioAnalysisSummaryResult(
                calculateValuation(context),
                calculateDiversification(context),
                calculateRebalancing(context)
        );
    }

    /**
     * 과거 데이터를 기반으로 선택한 전략(거치식/적립식)에 따른 투자 성과를 시뮬레이션합니다.
     */
    @Override
    public BacktestResult runBacktest(BacktestPortfolioCommand command) {
        AnalysisContext context = dataLoader.loadContext(command.portfolioId(), command.memberId());
        
        // 종목별 목표 비중 추출
        Map<String, BigDecimal> weights = context.portfolio().getItems().stream()
                .collect(Collectors.toMap(PortfolioItem::getSymbol, PortfolioItem::getTargetWeight));
        List<String> symbols = new ArrayList<>(weights.keySet());

        // 최근 2년치 데이터 로딩 및 시뮬레이션 실행
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        SimulationData data = simulationDataProvider.loadData(symbols, command.benchmarkTicker(), start, end);

        BacktestStrategy strategy = BacktestStrategy.valueOf(command.strategy().toUpperCase());

        return strategy == BacktestStrategy.DCA ? 
                backtestEngine.runDCA(data, weights, command.amount()) : 
                backtestEngine.runLumpSum(data, weights, command.amount());
    }

    /**
     * 포트폴리오 구성 종목 간의 가격 변동 상관계수 행렬을 산출합니다.
     */
    @Override
    public Map<String, Map<String, BigDecimal>> getCorrelationMatrix(Long memberId, Long portfolioId) {
        AnalysisContext context = dataLoader.loadContext(portfolioId, memberId);
        List<String> symbols = context.getStockSymbols();
        if (symbols.size() < 2) return Map.of();

        // 과거 2년 수익률 기반으로 상관계수 계산
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        SimulationData data = simulationDataProvider.loadData(symbols, BenchmarkType.KOSPI.getTicker(), start, end);

        Map<String, List<BigDecimal>> returnsMap = new HashMap<>();
        for (String symbol : symbols) {
            List<BigDecimal> returns = FinanceCalculationUtil.calculateDailyReturns(data.stockPrices().get(symbol));
            if (!returns.isEmpty()) returnsMap.put(symbol, returns);
        }
        return correlationCalculator.calculateMatrix(returnsMap);
    }

    // ── 내부 계산 로직 (Private Helpers) ──────────────────────────────────────────

    /**
     * 실시간 시세를 반영한 총 평가 금액 및 손익 지표 계산
     */
    private PortfolioValuationResult calculateValuation(AnalysisContext context) {
        BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
        BigDecimal currentTotalValue = BigDecimal.ZERO;
        BigDecimal previousTotalValue = BigDecimal.ZERO;

        for (PortfolioItem item : context.portfolio().getItems()) {
            BigDecimal purchaseAmount = item.calculatePurchaseAmount();
            totalPurchaseAmount = totalPurchaseAmount.add(purchaseAmount);

            if (item.getAssetType() == AssetType.STOCK) {
                StockPrice latestPrice = getLatestPrice(item.getSymbol(), context.priceMap());
                if (latestPrice != null) {
                    BigDecimal quantity = item.getQuantity();
                    BigDecimal closePrice = latestPrice.getClosePrice();
                    BigDecimal prevClosePrice = latestPrice.getPreviousClosePrice();
                    
                    currentTotalValue = currentTotalValue.add(quantity.multiply(closePrice));
                    BigDecimal basePrevPrice = (prevClosePrice != null) ? prevClosePrice : closePrice;
                    previousTotalValue = previousTotalValue.add(quantity.multiply(basePrevPrice));
                } else {
                    currentTotalValue = currentTotalValue.add(purchaseAmount);
                    previousTotalValue = previousTotalValue.add(purchaseAmount);
                }
            } else {
                currentTotalValue = currentTotalValue.add(item.getQuantity());
                previousTotalValue = previousTotalValue.add(item.getQuantity());
            }
        }

        // 제로 가치 포트폴리오 처리
        PortfolioStats stats = context.stats();
        if (totalPurchaseAmount.compareTo(BigDecimal.ZERO) == 0 && currentTotalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new PortfolioValuationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                (stats != null) ? stats.getMdd() : BigDecimal.ZERO,
                (stats != null) ? stats.getSharpeRatio() : BigDecimal.ZERO,
                (stats != null) ? stats.getBeta() : BigDecimal.ZERO
            );
        }

        BigDecimal totalProfitLoss = currentTotalValue.subtract(totalPurchaseAmount);
        BigDecimal dailyProfitLoss = currentTotalValue.subtract(previousTotalValue);

        return new PortfolioValuationResult(
                totalPurchaseAmount, currentTotalValue, totalProfitLoss, 
                FinanceCalculationUtil.calculateRate(totalProfitLoss, totalPurchaseAmount),
                dailyProfitLoss, FinanceCalculationUtil.calculateRate(dailyProfitLoss, previousTotalValue),
                (stats != null) ? stats.getMdd() : BigDecimal.ZERO,
                (stats != null) ? stats.getSharpeRatio() : BigDecimal.ZERO,
                (stats != null) ? stats.getBeta() : BigDecimal.ZERO
        );
    }

    /**
     * 자산군/업종/국가별 그룹핑을 통한 분산 비중 산출
     */
    private PortfolioDiversificationResult calculateDiversification(AnalysisContext context) {
        BigDecimal totalValue = BigDecimal.ZERO;
        
        // 최적화: EnumMap 활용 및 name() 호출 최소화
        Map<AssetType, BigDecimal> assetValueMap = new EnumMap<>(AssetType.class);
        Map<String, BigDecimal> sectorValueMap = new HashMap<>();
        Map<Country, BigDecimal> countryValueMap = new EnumMap<>(Country.class);

        for (PortfolioItem item : context.portfolio().getItems()) {
            BigDecimal currentValue = calculateCurrentValue(item, context.priceMap());
            totalValue = totalValue.add(currentValue);

            // 1. 자산군별 합산
            AssetType assetType = item.getAssetType();
            assetValueMap.put(assetType, assetValueMap.getOrDefault(assetType, BigDecimal.ZERO).add(currentValue));

            if (assetType == AssetType.STOCK) {
                Stock stock = context.stockMap().get(item.getSymbol());
                if (stock != null) {
                    // 2. 업종별 합산
                    String sector = stock.getSector().getSectorName();
                    sectorValueMap.put(sector, sectorValueMap.getOrDefault(sector, BigDecimal.ZERO).add(currentValue));
                    // 3. 국가별 합산
                    Country country = PortfolioMapperUtil.resolveCountry(stock.getMarketType());
                    countryValueMap.put(country, countryValueMap.getOrDefault(country, BigDecimal.ZERO).add(currentValue));
                }
            } else {
                // 현금 자산의 국가 합산 (통화 기준)
                Country country = PortfolioMapperUtil.resolveCountryFromCurrency(item.getCurrency());
                countryValueMap.put(country, countryValueMap.getOrDefault(country, BigDecimal.ZERO).add(currentValue));
            }
        }

        return new PortfolioDiversificationResult(totalValue, 
                convertAssetRatios(assetValueMap, totalValue),
                PortfolioMapperUtil.calculateRatios(sectorValueMap, totalValue), 
                convertCountryRatios(countryValueMap, totalValue));
    }

    private Map<String, BigDecimal> convertAssetRatios(Map<AssetType, BigDecimal> assetValueMap, BigDecimal total) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        assetValueMap.forEach((k, v) -> ratios.put(k.name(), FinanceCalculationUtil.calculateRate(v, total)));
        return ratios;
    }

    private Map<String, BigDecimal> convertCountryRatios(Map<Country, BigDecimal> countryValueMap, BigDecimal total) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        countryValueMap.forEach((k, v) -> ratios.put(k.name(), FinanceCalculationUtil.calculateRate(v, total)));
        return ratios;
    }

    /**
     * 현재 비중과 목표 비중을 비교하여 필요 매매 수량 산출
     */
    private PortfolioRebalancingResult calculateRebalancing(AnalysisContext context) {
        BigDecimal totalValue = BigDecimal.ZERO;
        for (PortfolioItem item : context.portfolio().getItems()) {
            totalValue = totalValue.add(calculateCurrentValue(item, context.priceMap()));
        }

        List<PortfolioRebalancingResult.RebalancingItem> items = new ArrayList<>();
        for (PortfolioItem item : context.portfolio().getItems()) {
            BigDecimal currentPrice = getCurrentPrice(item, context.priceMap());
            BigDecimal currentValue = calculateCurrentValue(item, context.priceMap());
            
            // 현재 비중 (%)
            BigDecimal currentWeight = totalValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    FinanceCalculationUtil.calculateRate(currentValue, totalValue);
            
            // 목표 비중 (%)
            BigDecimal targetWeight = item.getTargetWeight();
            
            // 목표 가치 = 전체 가치 * (목표 비중 / 100)
            BigDecimal targetValue = totalValue.multiply(targetWeight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal diffValue = targetValue.subtract(currentValue);
            
            // 현재가 기준 추천 매매 수량 (양수: 매수, 음수: 매도)
            BigDecimal recommendedQuantity = currentPrice.compareTo(BigDecimal.ZERO) > 0 ?
                    diffValue.divide(currentPrice, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            items.add(new PortfolioRebalancingResult.RebalancingItem(item.getSymbol(), currentWeight, targetWeight, 
                    targetWeight.subtract(currentWeight), item.getQuantity(), recommendedQuantity, currentPrice));
        }
        return new PortfolioRebalancingResult(totalValue, items);
    }

    // ── 기초 조회 유틸리티 ────────────────────────────────────────────────────────

    private StockPrice getLatestPrice(String symbol, Map<String, List<StockPrice>> priceMap) {
        return priceMap.getOrDefault(symbol, List.of()).stream().findFirst().orElse(null);
    }

    private BigDecimal getCurrentPrice(PortfolioItem item, Map<String, List<StockPrice>> priceMap) {
        if (item.getAssetType() == AssetType.CASH) return BigDecimal.ONE;
        StockPrice price = getLatestPrice(item.getSymbol(), priceMap);
        return (price != null) ? price.getClosePrice() : item.getPurchasePrice();
    }

    private BigDecimal calculateCurrentValue(PortfolioItem item, Map<String, List<StockPrice>> priceMap) {
        if (item.getAssetType() == AssetType.CASH) return item.getQuantity();
        return item.getQuantity().multiply(getCurrentPrice(item, priceMap));
    }

    private List<BigDecimal> calculateDailyReturns(List<StockPriceResult> prices) {
        return FinanceCalculationUtil.calculateDailyReturns(prices);
    }
}
