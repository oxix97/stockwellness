package org.stockwellness.adapter.in.web.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.stockwellness.domain.portfolio.RebalancingPeriod;

import java.math.BigDecimal;
import java.util.Map;

public record BacktestRequest(
    @NotBlank String strategy, // LUMP_SUM, DCA
    @Positive BigDecimal amount,
    @NotBlank String benchmarkTicker,
    RebalancingPeriod rebalancingPeriod,
    Map<String, BigDecimal> weights // { "ticker": weight_percent }
) {}
