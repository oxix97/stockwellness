package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.service.portfolio.internal.*;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final PortfolioStatCalculator statCalculator = new PortfolioStatCalculator();

    /**
     * 한 청크의 포트폴리오들을 벌크 시세 로딩을 통해 효율적으로 업데이트합니다.
     */
    @Transactional
    public void updatePortfolioStatsBatch(List<? extends Portfolio> portfolios) {
        if (portfolios.isEmpty()) return;

        // 1. 청크 내 모든 종목 합집합 추출
        Set<String> allSymbols = portfolios.stream()
                .flatMap(p -> p.getItems().stream())
                .filter(item -> item.getAssetType() == org.stockwellness.domain.portfolio.AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .collect(Collectors.toSet());

        if (allSymbols.isEmpty()) return;

        // 2. 청크 전체 시세 데이터 단 한번 벌크 로딩
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        SimulationData chunkSharedData = simulationDataProvider.loadData(new java.util.ArrayList<>(allSymbols), "KOSPI", start, end);

        // 3. 각 포트폴리오별 통계 계산 (로딩된 데이터 재사용)
        for (Portfolio portfolio : portfolios) {
            try {
                processSinglePortfolio(portfolio, chunkSharedData, end);
            } catch (Exception e) {
                log.error("포트폴리오 {} 통계 업데이트 실패", portfolio.getId(), e);
            }
        }
    }

    private void processSinglePortfolio(Portfolio portfolio, SimulationData sharedData, LocalDate baseDate) {
        Map<String, BigDecimal> weights = portfolio.getItems().stream()
                .collect(Collectors.toMap(PortfolioItem::getSymbol, PortfolioItem::getTargetWeight));

        if (weights.isEmpty()) return;

        // 전체 데이터 중 해당 포트폴리오의 종목 데이터만 추출하여 필터링된 SimulationData 생성
        SimulationData filteredData = filterDataForPortfolio(sharedData, weights.keySet());
        
        BacktestResult result = backtestEngine.runLumpSum(filteredData, weights, BigDecimal.valueOf(1000000));
        
        List<BigDecimal> values = result.dailyResults().stream()
                .map(BacktestResult.DailyBacktestResult::totalValue)
                .toList();
        List<BigDecimal> pReturns = result.dailyResults().stream()
                .map(BacktestResult.DailyBacktestResult::returnRate)
                .toList();
        List<BigDecimal> mReturns = result.dailyResults().stream()
                .map(BacktestResult.DailyBacktestResult::benchmarkReturnRate)
                .toList();

        BigDecimal mdd = statCalculator.calculateMDD(values);
        BigDecimal sharpe = statCalculator.calculateSharpeRatio(pReturns);
        BigDecimal beta = statCalculator.calculateBeta(pReturns, mReturns);

        portfolioStatsRepository.findByPortfolioId(portfolio.getId())
                .ifPresentOrElse(
                        stats -> stats.update(baseDate, mdd, sharpe, beta),
                        () -> portfolioStatsRepository.save(PortfolioStats.create(portfolio, baseDate, mdd, sharpe, beta))
                );
    }

    private SimulationData filterDataForPortfolio(SimulationData sharedData, Set<String> symbols) {
        Map<String, List<org.stockwellness.application.port.in.stock.result.StockPriceResult>> filteredStockPrices = new HashMap<>();
        symbols.forEach(s -> filteredStockPrices.put(s, sharedData.stockPrices().get(s)));
        
        return new SimulationData(filteredStockPrices, sharedData.benchmarkPrices());
    }

    @Deprecated
    @Transactional
    public void updatePortfolioStats(Portfolio portfolio) {
        updatePortfolioStatsBatch(List.of(portfolio));
    }
}
