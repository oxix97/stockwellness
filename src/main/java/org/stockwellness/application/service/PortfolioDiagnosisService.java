package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioPort;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.in.portfolio.result.StockStatResult;

import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.diagnosis.type.BalanceScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.CashScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioDiagnosisService {

    private final LoadPortfolioPort loadPortfolioPort;
    private final LoadStockPort loadStockPort;
    private final LoadStockHistoryPort loadStockHistoryPort;
    private final StockStatCalculator stockStatCalculator;

    public PortfolioHealthResult diagnose(Long portfolioId) {
        Portfolio portfolio = loadPortfolioPort.findById(portfolioId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> isinCodes = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getIsinCode)
                .toList();

        Map<String, Stock> stockMap = loadStockPort.loadStocksByIsinCodes(isinCodes).stream()
                .collect(Collectors.toMap(Stock::getIsinCode, stock -> stock));

        Map<String, List<StockHistory>> historyMap = loadStockHistoryPort.loadRecentHistoriesBatch(isinCodes, 5);

        double totalDefense = 0;
        double totalAttack = 0;
        double totalEndurance = 0;
        double totalAgility = 0;

        int stockCount = 0;
        Set<MarketType> marketTypes = new HashSet<>();

        for (PortfolioItem item : portfolio.getItems()) {
            double weight = (double) item.getPieceCount() / Portfolio.MAX_PIECES;

            if (item.getAssetType() == AssetType.STOCK) {
                stockCount++;
                Stock stock = Optional.ofNullable(stockMap.get(item.getIsinCode()))
                        .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + item.getIsinCode()));

                marketTypes.add(stock.getMarketType());

                List<StockHistory> histories = historyMap.getOrDefault(item.getIsinCode(), List.of());
                StockStatResult stat = stockStatCalculator.calculate(stock, histories);

                totalDefense += stat.defense() * weight;
                totalAttack += stat.attack() * weight;
                totalEndurance += stat.endurance() * weight;
                totalAgility += stat.agility() * weight;
            } else if (item.getAssetType() == AssetType.CASH) {
                totalDefense += CashScorePolicy.DEFENSE.getScore() * weight;
                totalAttack += CashScorePolicy.ATTACK.getScore() * weight;
                totalEndurance += CashScorePolicy.ENDURANCE.getScore() * weight;
                totalAgility += CashScorePolicy.AGILITY.getScore() * weight;
            }
        }

        int balanceScore = BalanceScorePolicy.calculate(stockCount, marketTypes);

        Map<String, Integer> categories = new HashMap<>();
        categories.put(DiagnosisCategory.DEFENSE.getKey(), (int) Math.round(totalDefense));
        categories.put(DiagnosisCategory.ATTACK.getKey(), (int) Math.round(totalAttack));
        categories.put(DiagnosisCategory.ENDURANCE.getKey(), (int) Math.round(totalEndurance));
        categories.put(DiagnosisCategory.AGILITY.getKey(), (int) Math.round(totalAgility));
        categories.put(DiagnosisCategory.BALANCE.getKey(), balanceScore);

        double average = categories.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int overallScore = (int) Math.round(average);

        return new PortfolioHealthResult(
                overallScore,
                categories,
                List.of(),
                "",
                "",
                List.of()
        );
    }
}