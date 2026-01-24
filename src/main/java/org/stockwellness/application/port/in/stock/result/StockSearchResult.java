package org.stockwellness.application.port.in.stock.result;

public record StockSearchResult(
        String isinCode,
        String ticker,
        String name,
        String marketType,
        Long totalShares
) {
}
