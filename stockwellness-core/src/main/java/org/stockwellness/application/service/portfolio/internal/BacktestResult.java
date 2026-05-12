package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record BacktestResult(
    List<DailyBacktestResult> dailyResults,
    BigDecimal cagr,              // 연평균 수익률
    BigDecimal mdd,               // 최대 낙폭
    BigDecimal relativeMdd,       // 벤치마크 대비 상대 낙폭 (Portfolio MDD - Benchmark MDD)
    BigDecimal sharpeRatio,       // 샤프 지수
    BigDecimal totalReturnRate,   // 총 수익률
    BigDecimal volatility,        // 변동성 (표준편차)
    BigDecimal alpha,             // 초과 수익률 (vs Primary Benchmark)
    BigDecimal beta,              // 시장 민감도
    BigDecimal bestYearRate,      // 최고의 해 수익률
    BigDecimal worstYearRate,     // 최악의 해 수익률
    Map<String, BigDecimal> itemReturns, // 종목별 수익률 (기여도 산출용)
    List<IndexComparison> comparisons, // 다중 지수 비교 결과 추가
    String aiComment
) {
    public static BacktestResult empty() {
        return new BacktestResult(
            Collections.emptyList(),
            BigDecimal.ZERO, // cagr
            BigDecimal.ZERO, // mdd
            BigDecimal.ZERO, // relativeMdd
            BigDecimal.ZERO, // sharpeRatio
            BigDecimal.ZERO, // totalReturnRate
            BigDecimal.ZERO, // volatility
            BigDecimal.ZERO, // alpha
            BigDecimal.ZERO, // beta
            BigDecimal.ZERO, // bestYearRate
            BigDecimal.ZERO, // worstYearRate
            Collections.emptyMap(),
            Collections.emptyList(),
            null
        );
    }

    public record DailyBacktestResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        Map<String, BigDecimal> benchmarkReturnRates // Ticker -> ReturnRate (다중 지수)
    ) {}

    public record IndexComparison(
        String indexName,
        String ticker,
        BigDecimal totalReturn,
        BigDecimal alpha,
        BigDecimal beta,
        BigDecimal mdd,
        BigDecimal relativeMdd
    ) {}
}
