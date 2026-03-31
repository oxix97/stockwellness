package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockDetailResult(
        String isinCode,
        String ticker,
        String name,
        String sectorName,
        String marketType,
        Long totalShares,
        LocalDate baseDate,
        BigDecimal currentPrice,
        BigDecimal priceChange,
        BigDecimal fluctuationRate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        Long volume,
        BigDecimal tradingValue,
        BigDecimal marketCap,
        BigDecimal rsi14,
        BigDecimal ma20,
        String aiInsight,
        boolean isMarketOpen
) {
}
