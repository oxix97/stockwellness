package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;

/** Aggregated FINAL investor flow used to rank stocks. */
public record InvestorTradeRanking(long stockId, BigDecimal netQuantity, BigDecimal netAmount) {}
