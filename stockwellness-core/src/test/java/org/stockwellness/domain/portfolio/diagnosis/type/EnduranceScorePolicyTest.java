package org.stockwellness.domain.portfolio.diagnosis.type;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EnduranceScorePolicyTest {

    @Test
    @DisplayName("입력값이 Null이거나 이동평균선이 0이면 기본 점수 50점을 반환한다")
    void shouldReturnDefaultScoreWhenInputIsInvalid() {
        assertThat(EnduranceScorePolicy.calculate(null, new BigDecimal("100"))).isEqualTo(50);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("100"), null)).isEqualTo(50);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("100"), BigDecimal.ZERO)).isEqualTo(50);
    }

    @Test
    @DisplayName("이격도에 따라 올바른 인내심 점수를 산출한다")
    void shouldCalculateEnduranceScoreBasedOnDisparity() {
        // OPTIMAL (100% ~ 110%)
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("100"), new BigDecimal("100"))).isEqualTo(100);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("105"), new BigDecimal("100"))).isEqualTo(100);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("110"), new BigDecimal("100"))).isEqualTo(100);

        // OVERHEATED (> 110%)
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("111"), new BigDecimal("100"))).isEqualTo(70);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("150"), new BigDecimal("100"))).isEqualTo(70);

        // UNDERVALUED (< 100%)
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("99"), new BigDecimal("100"))).isEqualTo(40);
        assertThat(EnduranceScorePolicy.calculate(new BigDecimal("50"), new BigDecimal("100"))).isEqualTo(40);
    }
}
