package org.stockwellness.domain.stock.analysis;

public record MarketCondition(
    TrendStatus trendStatus,       // 상태: 정배열, 역배열, 혼조세
    CrossoverSignal signal,        // 이벤트: 골든크로스, 데드크로스, 없음
    String description             // (옵션) AI나 로그를 위한 간단 요약
) {}