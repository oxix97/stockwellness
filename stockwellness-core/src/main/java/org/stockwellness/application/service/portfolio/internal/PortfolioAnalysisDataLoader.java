package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortfolioAnalysisDataLoader {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final PortfolioStatsRepository portfolioStatsRepository;

    public AnalysisContext loadContext(Long portfolioId, Long memberId) {
        Portfolio portfolio = portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> symbols = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == org.stockwellness.domain.portfolio.AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        Map<String, Stock> stockMap = stockPort.loadStocksByTickers(symbols).stream()
                .collect(Collectors.toMap(Stock::getTicker, s -> s));

        Map<String, List<StockPrice>> priceMap = stockPricePort.loadRecentHistoriesBatch(symbols, 1);

        PortfolioStats stats = portfolioStatsRepository.findByPortfolioId(portfolioId).orElse(null);

        return new AnalysisContext(portfolio, stockMap, priceMap, stats);
    }
}
