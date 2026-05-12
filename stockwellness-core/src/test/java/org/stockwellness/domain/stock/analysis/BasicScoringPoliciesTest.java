package org.stockwellness.domain.stock.analysis;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("기본 기술적 점수 정책 단위 테스트")
class BasicScoringPoliciesTest {

    @Nested
    @DisplayName("RsiPolicy 테스트")
    class RsiPolicyTest {
        private final BasicScoringPolicies.RsiPolicy policy = new BasicScoringPolicies.RsiPolicy();

        @Test
        @DisplayName("RSI 30 미만이면 +15점을 반환한다")
        void evaluate_Oversold() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshotWithRsi(BigDecimal.valueOf(29.9));
            assertThat(policy.evaluate(snapshot)).isEqualTo(15);
        }

        @Test
        @DisplayName("RSI 70 초과면 -15점을 반환한다")
        void evaluate_Overbought() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshotWithRsi(BigDecimal.valueOf(70.1));
            assertThat(policy.evaluate(snapshot)).isEqualTo(-15);
        }

        @Test
        @DisplayName("RSI 30 이상 70 이하이면 0점을 반환한다")
        void evaluate_Neutral() {
            assertThat(policy.evaluate(createSnapshotWithRsi(BigDecimal.valueOf(30)))).isEqualTo(0);
            assertThat(policy.evaluate(createSnapshotWithRsi(BigDecimal.valueOf(70)))).isEqualTo(0);
            assertThat(policy.evaluate(createSnapshotWithRsi(BigDecimal.valueOf(50)))).isEqualTo(0);
        }

        private TechnicalScoreService.IndicatorSnapshot createSnapshotWithRsi(BigDecimal rsi) {
            return new TechnicalScoreService.IndicatorSnapshot(
                    null, rsi, null, null, null, null, null, false, false, false, null, null, null
            );
        }
    }

    @Nested
    @DisplayName("AdxPolicy 테스트")
    class AdxPolicyTest {
        private final BasicScoringPolicies.AdxPolicy policy = new BasicScoringPolicies.AdxPolicy();

        @Test
        @DisplayName("ADX > 25이고 정배열이면 +5점을 반환한다")
        void evaluate_StrongBullish() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(BigDecimal.valueOf(26), AlignmentStatus.PERFECT);
            assertThat(policy.evaluate(snapshot)).isEqualTo(5);
        }

        @Test
        @DisplayName("ADX > 25이고 역배열이면 -5점을 반환한다")
        void evaluate_StrongBearish() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(BigDecimal.valueOf(26), AlignmentStatus.REVERSE);
            assertThat(policy.evaluate(snapshot)).isEqualTo(-5);
        }

        @Test
        @DisplayName("ADX <= 25이면 0점을 반환한다")
        void evaluate_WeakTrend() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(BigDecimal.valueOf(25), AlignmentStatus.PERFECT);
            assertThat(policy.evaluate(snapshot)).isEqualTo(0);
        }

        private TechnicalScoreService.IndicatorSnapshot createSnapshot(BigDecimal adx, AlignmentStatus alignment) {
            return new TechnicalScoreService.IndicatorSnapshot(
                    alignment, null, adx, null, null, null, null, false, false, false, null, null, null
            );
        }
    }

    @Nested
    @DisplayName("MacdPolicy 테스트")
    class MacdPolicyTest {
        private final BasicScoringPolicies.MacdPolicy policy = new BasicScoringPolicies.MacdPolicy();

        @Test
        @DisplayName("MACD 골든크로스 발생 시 +10점을 반환한다")
        void evaluate_MacdGoldenCross() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(true, BigDecimal.valueOf(5), BigDecimal.valueOf(3));
            assertThat(policy.evaluate(snapshot)).isEqualTo(10);
        }

        @Test
        @DisplayName("MACD 데드크로스 발생 시 -10점을 반환한다")
        void evaluate_MacdDeadCross() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(true, BigDecimal.valueOf(3), BigDecimal.valueOf(5));
            assertThat(policy.evaluate(snapshot)).isEqualTo(-10);
        }

        @Test
        @DisplayName("크로스 플래그가 false이면 0점을 반환한다")
        void evaluate_NoCross() {
            TechnicalScoreService.IndicatorSnapshot snapshot = createSnapshot(false, BigDecimal.valueOf(5), BigDecimal.valueOf(3));
            assertThat(policy.evaluate(snapshot)).isEqualTo(0);
        }

        private TechnicalScoreService.IndicatorSnapshot createSnapshot(boolean isCross, BigDecimal macd, BigDecimal signal) {
            return new TechnicalScoreService.IndicatorSnapshot(
                    null, null, null, null, null, macd, signal, false, false, isCross, null, null, null
            );
        }
    }
}
