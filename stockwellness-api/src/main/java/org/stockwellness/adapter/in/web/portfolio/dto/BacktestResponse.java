package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.service.portfolio.internal.BacktestResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BacktestResponse(
    List<DailyResult> dailyResults,
    BigDecimal cagr,
    BigDecimal mdd,
    BigDecimal sharpeRatio,
    BigDecimal totalReturnRate,
    BigDecimal volatility,
    BigDecimal alpha, // Primary Benchmark Alpha
    BigDecimal beta,  // Primary Benchmark Beta
    BigDecimal bestYearRate,
    BigDecimal worstYearRate,
    List<IndexComparisonResponse> comparisons, // 다중 지수 비교 추가
    String aiComment
) {
    public record DailyResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        Map<String, BigDecimal> benchmarkReturnRates // 다중 지수 수익률
    ) {}

    public record IndexComparisonResponse(
        String indexName,
        String ticker,
        BigDecimal totalReturn,
        BigDecimal alpha,
        BigDecimal beta
    ) {}

    public static BacktestResponse from(BacktestResult result) {
        return new BacktestResponse(
            result.dailyResults().stream()
                .map(r -> new DailyResult(r.date(), r.totalValue(), r.totalInvested(), r.returnRate(), r.benchmarkReturnRates()))
                .toList(),
            result.cagr(),
            result.mdd(),
            result.sharpeRatio(),
            result.totalReturnRate(),
            result.volatility(),
            result.alpha(),
            result.beta(),
            result.bestYearRate(),
            result.worstYearRate(),
            result.comparisons().stream()
                .map(c -> new IndexComparisonResponse(c.indexName(), c.ticker(), c.totalReturn(), c.alpha(), c.beta()))
                .toList(),
            result.aiComment()
        );
    }
}
