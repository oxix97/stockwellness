package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.MarketBreadthItem;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;

@Component
public class MarketBreadthCalculator {

    public MarketBreadthSnapshot summarize(List<MarketBreadthItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        int advancing = 0;
        int declining = 0;
        int unchanged = 0;
        int highVolatility = 0;

        for (MarketBreadthItem item : items) {
            BigDecimal fluctuationRate = calculateFluctuationRate(item)
                    .setScale(MarketWeatherPolicy.DISPLAY_SCALE, RoundingMode.HALF_UP);

            if (fluctuationRate.compareTo(MarketWeatherPolicy.FLAT_THRESHOLD) > 0) {
                advancing++;
            } else if (fluctuationRate.compareTo(MarketWeatherPolicy.FLAT_THRESHOLD.negate()) < 0) {
                declining++;
            } else {
                unchanged++;
            }

            BigDecimal intradaySwing = calculateIntradaySwing(item);
            if (fluctuationRate.abs().compareTo(MarketWeatherPolicy.HIGH_VOLATILITY_THRESHOLD) >= 0
                    || intradaySwing.compareTo(MarketWeatherPolicy.INTRADAY_SWING_THRESHOLD) >= 0) {
                highVolatility++;
            }
        }

        BigDecimal totalDecimal = BigDecimal.valueOf(items.size());
        return new MarketBreadthSnapshot(
                items.size(),
                advancing,
                declining,
                unchanged,
                highVolatility,
                ratio(advancing, totalDecimal),
                ratio(declining, totalDecimal),
                ratio(highVolatility, totalDecimal)
        );
    }

    private BigDecimal calculateFluctuationRate(MarketBreadthItem item) {
        BigDecimal base = (item.previousClosePrice() != null && item.previousClosePrice().compareTo(BigDecimal.ZERO) > 0)
                ? item.previousClosePrice()
                : item.openPrice();

        if (base == null || base.compareTo(BigDecimal.ZERO) == 0 || item.closePrice() == null) {
            return BigDecimal.ZERO;
        }

        return item.closePrice().subtract(base)
                .divide(base, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateIntradaySwing(MarketBreadthItem item) {
        if (item.highPrice() == null || item.lowPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal base = (item.previousClosePrice() != null && item.previousClosePrice().compareTo(BigDecimal.ZERO) > 0)
                ? item.previousClosePrice()
                : item.openPrice();

        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return item.highPrice()
                .subtract(item.lowPrice())
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
