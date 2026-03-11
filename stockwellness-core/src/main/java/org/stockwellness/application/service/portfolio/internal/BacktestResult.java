package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResult(
    List<DailyBacktestResult> dailyResults
) {
    public record DailyBacktestResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        BigDecimal benchmarkReturnRate
    ) {}
}
