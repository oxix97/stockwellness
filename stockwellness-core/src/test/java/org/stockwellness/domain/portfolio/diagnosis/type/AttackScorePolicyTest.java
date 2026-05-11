package org.stockwellness.domain.portfolio.diagnosis.type;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AttackScorePolicyTest {

    @Test
    @DisplayName("RSI 범위와 MACD 방향에 따라 올바른 공격력 점수를 산출한다")
    void shouldCalculateAttackScoreBasedOnRsiAndMacd() {
        // STRONG_BUY (RSI >= 70)
        assertThat(AttackScorePolicy.calculate(new BigDecimal("70"), new BigDecimal("1"))).isEqualTo(100); // 90 + 10
        assertThat(AttackScorePolicy.calculate(new BigDecimal("70"), new BigDecimal("-1"))).isEqualTo(90);

        // BUY (50 <= RSI < 70)
        assertThat(AttackScorePolicy.calculate(new BigDecimal("50"), new BigDecimal("1"))).isEqualTo(80); // 70 + 10
        assertThat(AttackScorePolicy.calculate(new BigDecimal("50"), new BigDecimal("-1"))).isEqualTo(70);

        // NEUTRAL (RSI < 50)
        assertThat(AttackScorePolicy.calculate(new BigDecimal("49"), new BigDecimal("1"))).isEqualTo(50); // 40 + 10
        assertThat(AttackScorePolicy.calculate(new BigDecimal("49"), new BigDecimal("-1"))).isEqualTo(40);
    }

    @Test
    @DisplayName("입력값이 Null이면 기본 점수 50점을 반환한다")
    void shouldReturnDefaultScoreWhenInputIsNull() {
        assertThat(AttackScorePolicy.calculate(null, new BigDecimal("1"))).isEqualTo(50);
        assertThat(AttackScorePolicy.calculate(new BigDecimal("70"), null)).isEqualTo(50);
    }

    @Test
    @DisplayName("MACD가 정확히 0이면 보너스 점수를 부여한다")
    void shouldApplyBonusWhenMacdIsZero() {
        assertThat(AttackScorePolicy.calculate(new BigDecimal("50"), BigDecimal.ZERO)).isEqualTo(80);
    }
}
