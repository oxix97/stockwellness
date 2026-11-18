package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.stockwellness.domain.stock.price.PriceBasis;

public record PriceObservation(
        Long stockId,
        LocalDate tradeDate,
        String quoteScope,
        PriceBasis priceBasis,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        BigDecimal tradingAmount,
        String dataStatus,
        Integer dataRevision,
        BigDecimal adjustedClosePrice,
        BigDecimal previousClosePrice,
        BigDecimal ma5,
        BigDecimal ma20,
        BigDecimal ma60,
        BigDecimal ma120,
        BigDecimal rsi14,
        BigDecimal macd,
        BigDecimal macdSignal,
        BigDecimal adx,
        BigDecimal plusDi,
        BigDecimal minusDi
) {
    public PriceObservation(Long stockId, LocalDate tradeDate, String quoteScope, PriceBasis priceBasis,
            BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
            Long volume, BigDecimal tradingAmount, String dataStatus, Integer dataRevision) {
        this(stockId, tradeDate, quoteScope, priceBasis, openPrice, highPrice, lowPrice, closePrice, volume,
                tradingAmount, dataStatus, dataRevision, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }
}
