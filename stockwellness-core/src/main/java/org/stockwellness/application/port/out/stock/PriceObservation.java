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
        Integer dataRevision
) {}
