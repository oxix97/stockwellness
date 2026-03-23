package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResult(
    List<DailyBacktestResult> dailyResults,
    BigDecimal cagr,              // 연평균 수익률
    BigDecimal mdd,               // 최대 낙폭
    BigDecimal sharpeRatio,       // 샤프 지수
    BigDecimal totalReturnRate,   // 총 수익률
    BigDecimal volatility,        // 변동성 (표준편차)
    BigDecimal alpha,             // 초과 수익률 (vs Benchmark)
    BigDecimal beta,              // 시장 민감도
    BigDecimal bestYearRate,      // 최고의 해 수익률
    BigDecimal worstYearRate      // 최악의 해 수익률
) {
    public record DailyBacktestResult(
        LocalDate date,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal returnRate,
        BigDecimal benchmarkReturnRate
    ) {}
}
