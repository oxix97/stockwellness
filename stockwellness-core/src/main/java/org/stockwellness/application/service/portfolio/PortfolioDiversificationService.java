package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.PortfolioDiversificationUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioDiversificationService implements PortfolioDiversificationUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;

    @Override
    public PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId) {
        Portfolio portfolio = portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> symbols = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        Map<String, List<StockPrice>> priceMap = stockPricePort.loadRecentHistoriesBatch(symbols, 1);
        Map<String, Stock> stockMap = stockPort.loadStocksByTickers(symbols).stream()
                .collect(Collectors.toMap(Stock::getTicker, s -> s));

        BigDecimal totalValue = BigDecimal.ZERO;
        Map<String, BigDecimal> assetValues = new HashMap<>();
        Map<String, BigDecimal> sectorValues = new HashMap<>();
        Map<String, BigDecimal> countryValues = new HashMap<>();

        for (PortfolioItem item : portfolio.getItems()) {
            BigDecimal currentValue = calculateCurrentValue(item, priceMap);
            totalValue = totalValue.add(currentValue);

            // Asset Type Grouping
            String assetType = item.getAssetType().name();
            assetValues.put(assetType, assetValues.getOrDefault(assetType, BigDecimal.ZERO).add(currentValue));

            if (item.getAssetType() == AssetType.STOCK) {
                Stock stock = stockMap.get(item.getSymbol());
                if (stock != null) {
                    // Sector Grouping (Only for Stocks)
                    String sector = stock.getSector().getSectorName();
                    sectorValues.put(sector, sectorValues.getOrDefault(sector, BigDecimal.ZERO).add(currentValue));

                    // Country Grouping
                    String country = resolveCountry(stock.getMarketType());
                    countryValues.put(country, countryValues.getOrDefault(country, BigDecimal.ZERO).add(currentValue));
                }
            } else if (item.getAssetType() == AssetType.CASH) {
                String country = resolveCountryFromCurrency(item.getCurrency());
                countryValues.put(country, countryValues.getOrDefault(country, BigDecimal.ZERO).add(currentValue));
            }
        }

        return new PortfolioDiversificationResult(
                totalValue,
                calculateRatios(assetValues, totalValue),
                calculateRatios(sectorValues, totalValue),
                calculateRatios(countryValues, totalValue)
        );
    }

    private BigDecimal calculateCurrentValue(PortfolioItem item, Map<String, List<StockPrice>> priceMap) {
        if (item.getAssetType() == AssetType.STOCK) {
            StockPrice latestPrice = priceMap.getOrDefault(item.getSymbol(), List.of()).stream()
                    .findFirst()
                    .orElse(null);
            if (latestPrice != null) {
                return item.getQuantity().multiply(latestPrice.getClosePrice());
            }
            return item.calculatePurchaseAmount();
        }
        return item.getQuantity(); // CASH is amount
    }

    private String resolveCountry(MarketType marketType) {
        return switch (marketType) {
            case KOSPI, KOSDAQ -> "KR";
            case NASDAQ, NYSE, AMEX -> "US";
            default -> "ETC";
        };
    }

    private String resolveCountryFromCurrency(String currency) {
        return switch (currency) {
            case "KRW" -> "KR";
            case "USD" -> "US";
            case "JPY" -> "JP";
            default -> "ETC";
        };
    }

    private Map<String, BigDecimal> calculateRatios(Map<String, BigDecimal> values, BigDecimal total) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        if (total.compareTo(BigDecimal.ZERO) == 0) return ratios;

        values.forEach((key, value) -> {
            BigDecimal ratio = value.divide(total, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
            ratios.put(key, ratio);
        });
        return ratios;
    }
}
