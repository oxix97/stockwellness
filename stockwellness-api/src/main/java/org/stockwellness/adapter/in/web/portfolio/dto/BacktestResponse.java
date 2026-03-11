package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.service.portfolio.internal.BacktestResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResponse(
    List<DailyResult> dailyResults
) {
    public record DailyResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        BigDecimal benchmarkReturnRate
    ) {}

    public static BacktestResponse from(BacktestResult result) {
        return new BacktestResponse(
            result.dailyResults().stream()
                .map(r -> new DailyResult(r.date(), r.totalValue(), r.totalInvested(), r.returnRate(), r.benchmarkReturnRate()))
                .toList()
        );
    }
}
