package org.stockwellness.domain.stock.insight;

import org.stockwellness.domain.stock.price.StockPrice;

import java.io.Serializable;
import java.math.BigDecimal;

public record LeadingStock(
        String name,
        String ticker,
        BigDecimal fluctuationRate,
        Long tradeVolume,
        BigDecimal transactionAmt // StockPrice 와 동일하게 변경
) implements Serializable {
    public static LeadingStock from(StockPrice p) {
        return new LeadingStock(
                p.getStock().getName(),
                p.getStock().getTicker(),
                p.getFluctuationRate(),
                p.getVolume(),
                p.getTransactionAmt()
        );
    }
}
