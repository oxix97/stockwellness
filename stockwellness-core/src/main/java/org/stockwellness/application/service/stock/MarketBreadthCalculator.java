package org.stockwellness.application.service.stock;

import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class MarketBreadthCalculator {

    public MarketBreadthSnapshot summarize(List<StockPrice> stockPrices) {
        if (stockPrices == null || stockPrices.isEmpty()) {
            return null;
        }

        int advancing = 0;
        int declining = 0;
        int unchanged = 0;
        int highVolatility = 0;

        for (StockPrice stockPrice : stockPrices) {
            BigDecimal fluctuationRate = stockPrice.getFluctuationRate()
                    .setScale(MarketWeatherPolicy.DISPLAY_SCALE, RoundingMode.HALF_UP);

            if (fluctuationRate.compareTo(MarketWeatherPolicy.FLAT_THRESHOLD) > 0) {
                advancing++;
            } else if (fluctuationRate.compareTo(MarketWeatherPolicy.FLAT_THRESHOLD.negate()) < 0) {
                declining++;
            } else {
                unchanged++;
            }

            BigDecimal intradaySwing = calculateIntradaySwing(stockPrice);
            if (fluctuationRate.abs().compareTo(MarketWeatherPolicy.HIGH_VOLATILITY_THRESHOLD) >= 0
                    || intradaySwing.compareTo(MarketWeatherPolicy.INTRADAY_SWING_THRESHOLD) >= 0) {
                highVolatility++;
            }
        }

        BigDecimal totalDecimal = BigDecimal.valueOf(stockPrices.size());
        return new MarketBreadthSnapshot(
                stockPrices.size(),
                advancing,
                declining,
                unchanged,
                highVolatility,
                ratio(advancing, totalDecimal),
                ratio(declining, totalDecimal),
                ratio(highVolatility, totalDecimal)
        );
    }

    private BigDecimal calculateIntradaySwing(StockPrice stockPrice) {
        if (stockPrice.getHighPrice() == null || stockPrice.getLowPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal base = stockPrice.getPreviousClosePrice();
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            base = stockPrice.getOpenPrice();
        }
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return stockPrice.getHighPrice()
                .subtract(stockPrice.getLowPrice())
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal ratio(int count, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count).divide(total, 4, RoundingMode.HALF_UP);
    }
}
