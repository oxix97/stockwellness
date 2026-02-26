package org.stockwellness.domain.stock.insight;

import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;

public record LeadingStock(
        String ticker,
        String name,
        BigDecimal fluctuationRate, // 등락률
        Long tradeVolume,            // 거래량
        BigDecimal tradeAmount      // 거래대금
) {
    public static LeadingStock from(StockPrice p) {
        return new LeadingStock(
                p.getStock().getTicker(),
                p.getStock().getName(),
                p.getFluctuationRate(),
                p.getVolume(),
                p.getTransactionAmount()
        );
    }
}