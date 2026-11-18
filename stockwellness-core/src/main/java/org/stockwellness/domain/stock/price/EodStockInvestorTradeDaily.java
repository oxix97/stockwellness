package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One provider-sourced investor flow for one stock, day, quote scope, and investor type. */
public record EodStockInvestorTradeDaily(
        long stockId,
        LocalDate tradeDate,
        String quoteScope,
        InvestorSourceType investorType,
        BigDecimal netQuantity,
        BigDecimal netAmount,
        String dataStatus,
        String source,
        Instant collectedAt
) {
    public EodStockInvestorTradeDaily {
        if (stockId <= 0 || tradeDate == null || quoteScope == null || quoteScope.isBlank() || quoteScope.length() > 20
                || investorType == null || !("FINAL".equals(dataStatus) || "PROVISIONAL".equals(dataStatus)
                || "INCOMPLETE".equals(dataStatus)) || source == null || source.isBlank() || source.length() > 100
                || collectedAt == null) {
            throw new IllegalArgumentException("A complete investor observation key and provenance are required");
        }
        if ("FINAL".equals(dataStatus) && (netQuantity == null || netAmount == null)) {
            throw new IllegalArgumentException("Final investor observations require quantity and amount");
        }
    }
}
