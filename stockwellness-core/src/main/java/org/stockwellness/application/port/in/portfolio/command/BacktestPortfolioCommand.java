package org.stockwellness.application.port.in.portfolio.command;

import org.stockwellness.domain.portfolio.RebalancingPeriod;

import java.math.BigDecimal;
import java.util.Map;

public record BacktestPortfolioCommand(
    Long memberId,
    Long portfolioId,
    String strategy, // LUMP_SUM, DCA
    BigDecimal amount,
    String benchmarkTicker,
    RebalancingPeriod rebalancingPeriod,
    Map<String, BigDecimal> weights // Ticker -> Percentage
) {}
