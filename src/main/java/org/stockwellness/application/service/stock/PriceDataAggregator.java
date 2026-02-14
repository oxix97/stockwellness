package org.stockwellness.application.service.stock;

import org.stockwellness.application.port.in.stock.result.ChartDataResponse.ChartPoint;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 일봉 데이터를 주봉/월봉으로 변환하는 유틸리티 클래스
 */
public class PriceDataAggregator {

    public static List<ChartPoint> aggregateToWeekly(List<StockPriceResult> dailyPrices) {
        // 월요일 기준으로 그룹화
        Map<LocalDate, List<StockPriceResult>> grouped = dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        p -> p.baseDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static List<ChartPoint> aggregateToMonthly(List<StockPriceResult> dailyPrices) {
        // 월 초 기준으로 그룹화
        Map<LocalDate, List<StockPriceResult>> grouped = dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        p -> p.baseDate().with(TemporalAdjusters.firstDayOfMonth())
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static ChartPoint aggregate(LocalDate date, List<StockPriceResult> prices) {
        List<StockPriceResult> sorted = prices.stream()
                .sorted((p1, p2) -> p1.baseDate().compareTo(p2.baseDate()))
                .toList();

        StockPriceResult first = sorted.get(0);
        StockPriceResult last = sorted.get(sorted.size() - 1);

        java.math.BigDecimal high = sorted.stream()
                .map(StockPriceResult::highPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::max);

        java.math.BigDecimal low = sorted.stream()
                .map(StockPriceResult::lowPrice)
                .reduce(first.lowPrice(), java.math.BigDecimal::min);

        long totalVolume = sorted.stream()
                .mapToLong(StockPriceResult::volume)
                .sum();

        return new ChartPoint(
                date,
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                last.adjClosePrice(),
                totalVolume
        );
    }
}
