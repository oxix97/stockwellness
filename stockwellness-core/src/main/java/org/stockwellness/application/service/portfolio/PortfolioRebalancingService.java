package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.PortfolioRebalancingUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioRebalancingService implements PortfolioRebalancingUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPricePort stockPricePort;

    @Override
    public PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId) {
        Portfolio portfolio = portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> symbols = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        Map<String, List<StockPrice>> priceMap = stockPricePort.loadRecentHistoriesBatch(symbols, 1);

        // 1. 전체 현재 가치 계산
        BigDecimal totalValue = BigDecimal.ZERO;
        for (PortfolioItem item : portfolio.getItems()) {
            totalValue = totalValue.add(getCurrentValue(item, priceMap));
        }

        List<PortfolioRebalancingResult.RebalancingItem> items = new ArrayList<>();

        // 2. 종목별 괴리율 및 추천 매매 수량 계산
        for (PortfolioItem item : portfolio.getItems()) {
            BigDecimal currentPrice = getCurrentPrice(item, priceMap);
            BigDecimal currentValue = getCurrentValue(item, priceMap);
            
            BigDecimal currentWeight = totalValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    currentValue.divide(totalValue, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal targetWeight = item.getTargetWeight();
            BigDecimal diffWeight = targetWeight.subtract(currentWeight);
            
            // 목표 가치 = 전체 가치 * (목표 비중 / 100)
            BigDecimal targetValue = totalValue.multiply(targetWeight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal diffValue = targetValue.subtract(currentValue);
            
            // 추천 수량 = (목표 가치 - 현재 가치) / 현재가
            BigDecimal recommendedQuantity = BigDecimal.ZERO;
            if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                recommendedQuantity = diffValue.divide(currentPrice, 4, RoundingMode.HALF_UP);
            }

            items.add(new PortfolioRebalancingResult.RebalancingItem(
                    item.getSymbol(),
                    currentWeight,
                    targetWeight,
                    diffWeight,
                    item.getQuantity(),
                    recommendedQuantity,
                    currentPrice
            ));
        }

        return new PortfolioRebalancingResult(totalValue, items);
    }

    private BigDecimal getCurrentPrice(PortfolioItem item, Map<String, List<StockPrice>> priceMap) {
        if (item.getAssetType() == AssetType.CASH) return BigDecimal.ONE;
        
        return priceMap.getOrDefault(item.getSymbol(), List.of()).stream()
                .findFirst()
                .map(StockPrice::getClosePrice)
                .orElse(item.getPurchasePrice()); // 시세 없으면 매입가 기준
    }

    private BigDecimal getCurrentValue(PortfolioItem item, Map<String, List<StockPrice>> priceMap) {
        if (item.getAssetType() == AssetType.CASH) return item.getQuantity();
        
        BigDecimal price = getCurrentPrice(item, priceMap);
        return item.getQuantity().multiply(price);
    }
}
