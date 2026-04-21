package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final PortfolioPort portfolioPort;
    private final PlatformTransactionManager transactionManager;

    private static final int MAX_SYMBOLS_PER_LOAD = 50; // 메모리 보호를 위한 임계치

    @Transactional
    public void updatePortfolioStatsBatch(List<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) return;

        List<Portfolio> portfolios = portfolioPort.loadAllWithItems(portfolioIds);
        if (portfolios.isEmpty()) return;

        long startTime = System.currentTimeMillis();
        int totalCount = portfolios.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Set<String> allSymbols = portfolios.stream()
                .flatMap(p -> p.getItems().stream())
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .collect(Collectors.toSet());

        if (allSymbols.isEmpty()) return;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        SimulationData chunkSharedData = loadPartitionedData(allSymbols, start, end);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // Virtual Thread 기반 병렬 처리 (spring.threads.virtual.enabled: true 전제)
        List<CompletableFuture<Void>> futures = portfolios.stream()
                .map(portfolio -> CompletableFuture.runAsync(() -> {
                    try {
                        transactionTemplate.executeWithoutResult(status -> {
                            processSinglePortfolio(portfolio, chunkSharedData, end);
                        });
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("[배치] 포트폴리오 {} 통계 업데이트 실패: {}", portfolio.getId(), e.getMessage());
                        failureCount.incrementAndGet();
                    }
                }))
                .toList();

        // 모든 병렬 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        log.info("[배치 성능 모니터링] PortfolioStatsBatchJob Chunk 완료. 소요시간: {}ms, 성공: {}, 실패: {}, 총계: {}, 가상스레드사용: {}",
                duration, successCount.get(), failureCount.get(), totalCount, Thread.currentThread().isVirtual());
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
        BacktestResult result = backtestEngine.runLumpSum(filteredData, weights, BigDecimal.valueOf(1000000), RebalancingPeriod.NONE, BenchmarkType.KOSPI.getTicker(), BigDecimal.valueOf(3.0), true);
        
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


}
