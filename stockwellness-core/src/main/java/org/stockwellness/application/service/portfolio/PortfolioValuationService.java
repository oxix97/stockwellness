package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.PortfolioValuationUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioValuationService implements PortfolioValuationUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPricePort stockPricePort;

    @Override
    public PortfolioValuationResult getValuation(Long memberId, Long portfolioId) {
        Portfolio portfolio = portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> symbols = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        Map<String, List<StockPrice>> priceMap = stockPricePort.loadRecentHistoriesBatch(symbols, 1);

        BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
        BigDecimal currentTotalValue = BigDecimal.ZERO;
        BigDecimal previousTotalValue = BigDecimal.ZERO;

        for (PortfolioItem item : portfolio.getItems()) {
            BigDecimal quantity = item.getQuantity();
            BigDecimal purchaseAmount = item.calculatePurchaseAmount();
            totalPurchaseAmount = totalPurchaseAmount.add(purchaseAmount);

            if (item.getAssetType() == AssetType.STOCK) {
                StockPrice latestPrice = priceMap.getOrDefault(item.getSymbol(), List.of()).stream()
                        .findFirst()
                        .orElse(null);

                if (latestPrice != null) {
                    BigDecimal closePrice = latestPrice.getClosePrice();
                    BigDecimal prevClosePrice = latestPrice.getPreviousClosePrice();

                    currentTotalValue = currentTotalValue.add(quantity.multiply(closePrice));
                    
                    // 전일 종가가 없으면 현재가로 대체하여 변동 없음을 나타냄
                    BigDecimal basePrevPrice = (prevClosePrice != null) ? prevClosePrice : closePrice;
                    previousTotalValue = previousTotalValue.add(quantity.multiply(basePrevPrice));
                } else {
                    // 시세 정보가 없으면 매입가 기준으로 유지 (보수적 접근)
                    currentTotalValue = currentTotalValue.add(purchaseAmount);
                    previousTotalValue = previousTotalValue.add(purchaseAmount);
                }
            } else if (item.getAssetType() == AssetType.CASH) {
                currentTotalValue = currentTotalValue.add(quantity);
                previousTotalValue = previousTotalValue.add(quantity);
            }
        }

        BigDecimal totalProfitLoss = currentTotalValue.subtract(totalPurchaseAmount);
        BigDecimal totalReturnRate = calculateRate(totalProfitLoss, totalPurchaseAmount);

        BigDecimal dailyProfitLoss = currentTotalValue.subtract(previousTotalValue);
        BigDecimal dailyReturnRate = calculateRate(dailyProfitLoss, previousTotalValue);

        return new PortfolioValuationResult(
                totalPurchaseAmount,
                currentTotalValue,
                totalProfitLoss,
                totalReturnRate,
                dailyProfitLoss,
                dailyReturnRate
        );
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
