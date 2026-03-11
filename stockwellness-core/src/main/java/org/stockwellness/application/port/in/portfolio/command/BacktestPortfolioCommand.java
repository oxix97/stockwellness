package org.stockwellness.application.port.in.portfolio.command;

import java.math.BigDecimal;

public record BacktestPortfolioCommand(
    Long memberId,
    Long portfolioId,
    String strategy, // LUMP_SUM, DCA
    BigDecimal amount,
    String benchmarkTicker
) {}
