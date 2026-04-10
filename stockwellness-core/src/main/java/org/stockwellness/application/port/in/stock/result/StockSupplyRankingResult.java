package org.stockwellness.application.port.in.stock.result;

import java.math.BigDecimal;

public record StockSupplyRankingResult(
        String ticker,
        String stockName,
        String sectorName,
        BigDecimal currentPrice,
        BigDecimal fluctuationRate,
        Long netBuyingQuantity,
        BigDecimal netBuyingAmount,
        BigDecimal transactionAmount
) {
}
