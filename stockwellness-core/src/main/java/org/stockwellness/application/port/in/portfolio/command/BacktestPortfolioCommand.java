package org.stockwellness.application.port.in.portfolio.command;

import org.stockwellness.domain.portfolio.RebalancingPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BacktestPortfolioCommand(
    Long memberId,
    Long portfolioId,
    String strategy, // LUMP_SUM, DCA
    BigDecimal amount,
    List<String> benchmarkTickers,
    RebalancingPeriod rebalancingPeriod,
    Map<String, BigDecimal> weights // Ticker -> Percentage
) {}
