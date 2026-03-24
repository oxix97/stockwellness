package org.stockwellness.application.port.in.portfolio.command;

import java.math.BigDecimal;
import java.util.Map;

public record BacktestPortfolioCommand(
    Long memberId,
    Long portfolioId,
    String strategy, // LUMP_SUM, DCA
    BigDecimal amount,
    String benchmarkTicker,
    String rebalancingPeriod, // NONE, MONTHLY, QUARTERLY, YEARLY
    Map<String, BigDecimal> weights // Ticker -> Percentage
) {}
