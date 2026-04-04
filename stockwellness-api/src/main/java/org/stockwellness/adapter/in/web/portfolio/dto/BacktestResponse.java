package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.service.portfolio.internal.BacktestResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record BacktestResponse(
    List<DailyResult> dailyResults,
    BigDecimal cagr,
    BigDecimal mdd,
    BigDecimal relativeMdd,
    BigDecimal sharpeRatio,
    BigDecimal totalReturnRate,
    BigDecimal volatility,
    BigDecimal alpha, // Primary Benchmark Alpha
    BigDecimal beta,  // Primary Benchmark Beta
    BigDecimal bestYearRate,
    BigDecimal worstYearRate,
    Map<String, BigDecimal> itemReturns,
    List<IndexComparisonResponse> comparisons, // 다중 지수 비교 추가
    String aiComment
) {
    public record DailyResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        BigDecimal benchmarkReturnRate, // 주요 벤치마크 수익률 (FE 호환용 스칼라)
        Map<String, BigDecimal> benchmarkReturnRates // 다중 지수 수익률
    ) {}

    public record IndexComparisonResponse(
        String indexName,
        String ticker,
        BigDecimal totalReturn,
        BigDecimal alpha,
        BigDecimal beta,
        BigDecimal mdd,
        BigDecimal relativeMdd
    ) {}

    public static BacktestResponse from(BacktestResult result, String primaryBenchmarkTicker) {
        return new BacktestResponse(
            result.dailyResults().stream()
                .map(r -> new DailyResult(
                    r.date(), 
                    r.totalValue().setScale(0, RoundingMode.HALF_UP), 
                    r.totalInvested().setScale(0, RoundingMode.HALF_UP), 
                    r.returnRate().setScale(4, RoundingMode.HALF_UP),
                    r.benchmarkReturnRates().getOrDefault(primaryBenchmarkTicker, BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP),
                    r.benchmarkReturnRates().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().setScale(4, RoundingMode.HALF_UP)))
                ))
                .toList(),
            result.cagr().setScale(4, RoundingMode.HALF_UP),
            result.mdd().setScale(4, RoundingMode.HALF_UP),
            result.relativeMdd().setScale(4, RoundingMode.HALF_UP),
            result.sharpeRatio().setScale(4, RoundingMode.HALF_UP),
            result.totalReturnRate().setScale(4, RoundingMode.HALF_UP),
            result.volatility().setScale(4, RoundingMode.HALF_UP),
            result.alpha().setScale(4, RoundingMode.HALF_UP),
            result.beta().setScale(4, RoundingMode.HALF_UP),
            result.bestYearRate().setScale(4, RoundingMode.HALF_UP),
            result.worstYearRate().setScale(4, RoundingMode.HALF_UP),
            result.itemReturns().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().setScale(4, RoundingMode.HALF_UP))),
            result.comparisons().stream()
                .map(c -> new IndexComparisonResponse(
                    c.indexName(), 
                    c.ticker(), 
                    c.totalReturn().setScale(4, RoundingMode.HALF_UP), 
                    c.alpha().setScale(4, RoundingMode.HALF_UP), 
                    c.beta().setScale(4, RoundingMode.HALF_UP), 
                    c.mdd().setScale(4, RoundingMode.HALF_UP), 
                    c.relativeMdd().setScale(4, RoundingMode.HALF_UP)))
                .toList(),
            result.aiComment()
        );
    }
}
