package org.stockwellness.adapter.in.web.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BacktestRequest(
    @NotBlank String strategy, // LUMP_SUM, DCA
    @Positive BigDecimal amount,
    @NotBlank String benchmarkTicker
) {}
