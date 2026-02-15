package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse.ChartPoint;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceDataAggregatorTest {

    @Test
    @DisplayName("일봉 데이터를 주봉으로 정확하게 집계한다")
    void aggregateToWeekly() {
        // Given: 월~금 데이터
        List<StockPriceResult> daily = List.of(
                createPrice("2024-02-05", 100, 110, 90, 105, 10), // 월
                createPrice("2024-02-06", 105, 115, 100, 110, 10),
                createPrice("2024-02-07", 110, 120, 105, 115, 10),
                createPrice("2024-02-08", 115, 125, 110, 120, 10),
                createPrice("2024-02-09", 120, 130, 115, 125, 10)  // 금
        );

        // When
        List<ChartPoint> weekly = PriceDataAggregator.aggregateToWeekly(daily);

        // Then
        assertThat(weekly).hasSize(1);
        ChartPoint result = weekly.get(0);
        assertThat(result.date()).isEqualTo(LocalDate.parse("2024-02-05")); // 해당 주의 월요일
        assertThat(result.open()).isEqualByComparingTo("100");
        assertThat(result.high()).isEqualByComparingTo("130");
        assertThat(result.low()).isEqualByComparingTo("90");
        assertThat(result.close()).isEqualByComparingTo("125");
        assertThat(result.volume()).isEqualTo(50);
    }

    @Test
    @DisplayName("일봉 데이터를 월봉으로 정확하게 집계한다")
    void aggregateToMonthly() {
        // Given: 2월 데이터 2개 (다른 주)
        List<StockPriceResult> daily = List.of(
                createPrice("2024-02-01", 100, 110, 90, 105, 10),
                createPrice("2024-02-28", 105, 150, 80, 140, 20)
        );

        // When
        List<ChartPoint> monthly = PriceDataAggregator.aggregateToMonthly(daily);

        // Then
        assertThat(monthly).hasSize(1);
        ChartPoint result = monthly.get(0);
        assertThat(result.date()).isEqualTo(LocalDate.parse("2024-02-01"));
        assertThat(result.open()).isEqualByComparingTo("100");
        assertThat(result.high()).isEqualByComparingTo("150");
        assertThat(result.low()).isEqualByComparingTo("80");
        assertThat(result.close()).isEqualByComparingTo("140");
        assertThat(result.volume()).isEqualTo(30);
    }

    private StockPriceResult createPrice(String date, double o, double h, double l, double c, long v) {
        return new StockPriceResult(
                LocalDate.parse(date),
                BigDecimal.valueOf(o),
                BigDecimal.valueOf(h),
                BigDecimal.valueOf(l),
                BigDecimal.valueOf(c),
                BigDecimal.valueOf(c), // adjClose
                v
        );
    }
}
