package org.stockwellness.adapter.in.web.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

public record BacktestRequest(
    @NotBlank String strategy, // LUMP_SUM, DCA
    @Positive BigDecimal amount,
    @NotBlank String benchmarkTicker,
    String rebalancingPeriod, // "NONE", "MONTHLY", "QUARTERLY", "YEARLY"
    Map<String, BigDecimal> weights // { "ticker": weight_percent }
) {}
