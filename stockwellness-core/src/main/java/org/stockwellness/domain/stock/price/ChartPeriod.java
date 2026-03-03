package org.stockwellness.domain.stock.price;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * 차트 조회 기간 및 시작일 계산 로직을 관리하는 Enum
 */
@Getter
@RequiredArgsConstructor
public enum ChartPeriod {
    ONE_WEEK("1W", end -> end.minusWeeks(1)),
    ONE_MONTH("1M", end -> end.minusMonths(1)),
    THREE_MONTHS("3M", end -> end.minusMonths(3)),
    ONE_YEAR("1Y", end -> end.minusYears(1)),
    THREE_YEARS("3Y", end -> end.minusYears(3)),
    FIVE_YEARS("5Y", end -> end.minusYears(5)),
    ALL("ALL", end -> LocalDate.of(1990, 1, 1));

    private final String label;
    private final Function<LocalDate, LocalDate> startCalculator;

    public LocalDate calculateStartDate(LocalDate end) {
        return startCalculator.apply(end);
    }

    public static ChartPeriod fromLabel(String label) {
        for (ChartPeriod period : values()) {
            if (period.label.equalsIgnoreCase(label)) {
                return period;
            }
        }
        return ONE_YEAR; // 기본값
    }
}
