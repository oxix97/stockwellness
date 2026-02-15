package org.stockwellness.domain.stock.analysis;

public enum CrossoverSignal {
    GOLDEN_CROSS, // 5일선이 20일선 상향 돌파
    DEAD_CROSS,   // 5일선이 20일선 하향 이탈
    NONE
}