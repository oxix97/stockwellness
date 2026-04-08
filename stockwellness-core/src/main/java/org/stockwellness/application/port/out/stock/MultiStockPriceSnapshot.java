package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;

public record MultiStockPriceSnapshot(
        String ticker,
        String name,
        BigDecimal closePrice,
        BigDecimal priceChange,
        BigDecimal priceChangeRate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        Long accumulatedVolume,
        BigDecimal accumulatedTradingValue,
        BigDecimal previousClosePrice,
        BigDecimal netInstitutionalBuyingAmt,
        BigDecimal netForeignBuyingAmt
) {
}
