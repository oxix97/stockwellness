package org.stockwellness.domain.portfolio.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FinancialMath 단위 테스트")
class FinancialMathTest {

    @Test
    @DisplayName("CAGR 계산 검증 (100 -> 121, 2년)")
    void calculateCAGR() {
        BigDecimal start = BigDecimal.valueOf(100);
        BigDecimal end = BigDecimal.valueOf(121);
        double years = 2.0;

        BigDecimal cagr = FinancialMath.calculateCAGR(start, end, years);

        // (121/100)^(1/2) - 1 = 1.1 - 1 = 0.1 -> 10%
        assertThat(cagr.doubleValue()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("표준편차 계산 검증")
    void calculateStandardDeviation() {
        List<BigDecimal> values = List.of(
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(30)
        );

        BigDecimal std = FinancialMath.calculateStandardDeviation(values);
        
        // mean = 20
        // variance = ((10-20)^2 + (20-20)^2 + (30-20)^2) / 3 = (100 + 0 + 100) / 3 = 66.6666...
        // std = sqrt(66.6666...) = 8.1649658...
        assertThat(std.doubleValue()).isEqualTo(8.1650);
        }

    @Test
    @DisplayName("변화율 계산 검증 (100 -> 110)")
    void calculateReturnRate() {
        BigDecimal start = BigDecimal.valueOf(100);
        BigDecimal end = BigDecimal.valueOf(110);

        BigDecimal rate = FinancialMath.calculateReturnRate(start, end);

        assertThat(rate.doubleValue()).isEqualTo(10.0);
    }
}
