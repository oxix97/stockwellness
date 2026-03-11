package org.stockwellness.application.service.portfolio.internal;

import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;
import java.util.Map;

import static org.stockwellness.domain.portfolio.AssetType.STOCK;

public record AnalysisContext(
    Portfolio portfolio,
    Map<String, Stock> stockMap,
    Map<String, List<StockPrice>> priceMap,
    PortfolioStats stats
) {
    public List<String> getStockSymbols() {
        return portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();
    }
}
