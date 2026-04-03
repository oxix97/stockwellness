package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.service.portfolio.internal.*;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.portfolio.RebalancingPeriod;
import org.stockwellness.domain.portfolio.event.PortfolioAnalysisCompletedEvent;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.global.util.FinanceCalculationUtil;
import org.stockwellness.global.util.JsonUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioStatBatchService {

    private final PortfolioStatsRepository portfolioStatsRepository;
    private final SimulationDataProvider simulationDataProvider;
    private final BacktestEngine backtestEngine;
    private final OutboxPort outboxPort;
    private final JsonUtil jsonUtil;
    private final PortfolioAnalysisService portfolioAnalysisService; // 벤치마크 계산 로직 공유

    private static final int MAX_SYMBOLS_PER_LOAD = 50; // 메모리 보호를 위한 임계치

    @Transactional
    public void updatePortfolioStatsBatch(List<? extends Portfolio> portfolios) {
        if (portfolios.isEmpty()) return;

        Set<String> allSymbols = portfolios.stream()
                .flatMap(p -> p.getItems().stream())
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .collect(Collectors.toSet());

        if (allSymbols.isEmpty()) return;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        SimulationData chunkSharedData = loadPartitionedData(allSymbols, start, end);

        for (Portfolio portfolio : portfolios) {
            try {
                processSinglePortfolio(portfolio, chunkSharedData, end);
            } catch (Exception e) {
                log.error("포트폴리오 {} 통계 업데이트 실패", portfolio.getId(), e);
            }
        }
    }

    private SimulationData loadPartitionedData(Set<String> allSymbols, LocalDate start, LocalDate end) {
        List<String> symbolList = new ArrayList<>(allSymbols);
        Map<String, List<StockPriceResult>> allStockPrices = new HashMap<>();
        Map<String, List<StockPriceResult>> benchmarkPrices = null;

        for (int i = 0; i < symbolList.size(); i += MAX_SYMBOLS_PER_LOAD) {
            List<String> partition = symbolList.subList(i, Math.min(i + MAX_SYMBOLS_PER_LOAD, symbolList.size()));
            SimulationData partData = simulationDataProvider.loadData(partition, null, start, end);
            allStockPrices.putAll(partData.stockPrices());
            if (benchmarkPrices == null) {
                benchmarkPrices = partData.benchmarkPrices();
            }
        }

        return new SimulationData(allStockPrices, benchmarkPrices != null ? benchmarkPrices : Map.of());
    }

    private void processSinglePortfolio(Portfolio portfolio, SimulationData sharedData, LocalDate baseDate) {
        Map<String, BigDecimal> weights = portfolio.getItems().stream()
                .collect(Collectors.toMap(PortfolioItem::getSymbol, PortfolioItem::getTargetWeight));

        if (weights.isEmpty()) return;

        SimulationData filteredData = filterDataForPortfolio(sharedData, weights.keySet());
        BacktestResult result = backtestEngine.runLumpSum(filteredData, weights, BigDecimal.valueOf(1000000), RebalancingPeriod.NONE, BenchmarkType.KOSPI.getTicker(), BigDecimal.valueOf(3.0));
        
        BigDecimal mdd = result.mdd();
        BigDecimal sharpe = result.sharpeRatio();
        BigDecimal beta = result.beta();
        
        // 리팩토링된 엔티티 메서드 활용을 위한 시세 맵 구성
        Map<String, BigDecimal> currentPrices = portfolio.getItems().stream()
                .collect(Collectors.toMap(PortfolioItem::getSymbol, i -> {
                    List<StockPriceResult> prices = filteredData.stockPrices().get(i.getSymbol());
                    return (prices != null && !prices.isEmpty()) ? prices.get(prices.size() - 1).closePrice() : i.getPurchasePrice();
                }));

        BigDecimal inceptionReturn = portfolio.calculateTotalReturnRate(currentPrices);
        BigDecimal benchmarkReturn = portfolioAnalysisService.calculateBenchmarkReturn(BenchmarkType.KOSPI.getTicker(), baseDate.minusYears(2), baseDate);

        portfolioStatsRepository.findByPortfolioId(portfolio.getId())
                .ifPresentOrElse(
                        stats -> stats.update(baseDate, mdd, sharpe, beta, inceptionReturn, benchmarkReturn),
                        () -> portfolioStatsRepository.save(PortfolioStats.create(portfolio, baseDate, mdd, sharpe, beta, inceptionReturn, benchmarkReturn))
                );

        String payload = jsonUtil.toJson(new PortfolioAnalysisCompletedEvent(
                portfolio.getId(), baseDate, mdd, sharpe, beta));
        outboxPort.save(OutboxEvent.create(
                KafkaTopicConfig.PORTFOLIO_ANALYSIS_COMPLETED_TOPIC, payload));
    }

    private SimulationData filterDataForPortfolio(SimulationData sharedData, Set<String> symbols) {
        Map<String, List<StockPriceResult>> filteredStockPrices = new HashMap<>();
        symbols.forEach(s -> {
            List<StockPriceResult> prices = sharedData.stockPrices().get(s);
            if (prices != null) filteredStockPrices.put(s, prices);
        });
        return new SimulationData(filteredStockPrices, sharedData.benchmarkPrices());
    }

    @Deprecated
    @Transactional
    public void updatePortfolioStats(Portfolio portfolio) {
        updatePortfolioStatsBatch(List.of(portfolio));
    }
}
