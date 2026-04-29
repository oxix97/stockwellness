package org.stockwellness.domain.portfolio.diagnosis.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class DefenseScorePolicyTest {

    @Test
    @DisplayName("시가총액 규모에 따라 올바른 방어력 점수를 산출한다")
    void shouldCalculateDefenseScoreBasedOnMarketCap() {
        // MEGA_CAP (>= 20조)
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("20000000000000"))).isEqualTo(100);
        
        // LARGE_CAP (>= 5조)
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("5000000000000"))).isEqualTo(80);
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("19999999999999"))).isEqualTo(80);

        // MID_CAP (>= 1조)
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("1000000000000"))).isEqualTo(60);
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("4999999999999"))).isEqualTo(60);

        // SMALL_CAP (< 1조)
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("999999999999"))).isEqualTo(40);
        assertThat(DefenseScorePolicy.calculate(new BigDecimal("0"))).isEqualTo(40);
    }

    @Test
    @DisplayName("입력값이 Null이면 기본 점수 50점을 반환한다")
    void shouldReturnDefaultScoreWhenInputIsNull() {
        assertThat(DefenseScorePolicy.calculate(null)).isEqualTo(50);
    }
}
