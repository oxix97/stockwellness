package org.stockwellness.domain.portfolio.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReturnSeries 단위 테스트")
class ReturnSeriesTest {

    @Test
    @DisplayName("MDD 계산 검증")
    void calculateMDD() {
        List<BigDecimal> values = List.of(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(110),
            BigDecimal.valueOf(105), // -4.545...%
            BigDecimal.valueOf(90),  // -18.18...% from peak(110)
            BigDecimal.valueOf(100)
        );

        BigDecimal mdd = ReturnSeries.calculateMDD(values);

        // (110 - 90) / 110 * 100 = 20 / 110 * 100 = 18.1818...%
        assertThat(mdd.doubleValue()).isEqualTo(18.1818);
    }

    @Test
    @DisplayName("TreeMap 정렬 검증")
    void treeMapSort() {
        Map<LocalDate, BigDecimal> input = Map.of(
            LocalDate.of(2026, 1, 3), BigDecimal.valueOf(3),
            LocalDate.of(2026, 1, 1), BigDecimal.valueOf(1),
            LocalDate.of(2026, 1, 2), BigDecimal.valueOf(2)
        );

        ReturnSeries series = new ReturnSeries(input);
        
        List<BigDecimal> returns = series.getReturnsOnly();
        assertThat(returns).containsExactly(
            BigDecimal.valueOf(1),
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(3)
        );
    }
}
