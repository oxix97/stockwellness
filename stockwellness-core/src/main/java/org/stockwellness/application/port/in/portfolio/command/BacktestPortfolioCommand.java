package org.stockwellness.application.port.in.portfolio.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.portfolio.RebalancingPeriod;
import org.stockwellness.domain.stock.price.ChartPeriod;

public record BacktestPortfolioCommand(
    Long memberId,
    Long portfolioId,
    String strategy, // LUMP_SUM, DCA
    BigDecimal amount,
    List<String> benchmarkTickers,
    ChartPeriod period,
    Boolean dividendReinvested,
    RebalancingPeriod rebalancingPeriod,
    Map<String, BigDecimal> weights // Ticker -> Percentage
) {}
