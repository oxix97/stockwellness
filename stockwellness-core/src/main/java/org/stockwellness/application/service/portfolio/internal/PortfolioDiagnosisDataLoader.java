package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortfolioDiagnosisDataLoader {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;

    public DiagnosisContext load(Long portfolioId) {
        Portfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> isinCodes = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getIsinCode)
                .toList();

        Map<String, Stock> stockMap = stockPort.loadStocksByTickers(isinCodes).stream()
                .collect(Collectors.toMap(Stock::getStandardCode, stock -> stock));

        Map<String, List<StockPrice>> stockPriceMap = stockPricePort.loadRecentHistoriesBatch(isinCodes, 5);

        return new DiagnosisContext(portfolio, stockMap, stockPriceMap);
    }
}