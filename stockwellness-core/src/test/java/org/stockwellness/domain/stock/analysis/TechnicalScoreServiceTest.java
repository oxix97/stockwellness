package org.stockwellness.domain.stock.analysis;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TechnicalScoreService 단위 테스트")
class TechnicalScoreServiceTest {

    private final TechnicalScoreService technicalScoreService = new TechnicalScoreService(List.of(
            new BasicScoringPolicies.AlignmentPolicy(),
            new BasicScoringPolicies.RsiPolicy(),
            new BasicScoringPolicies.EventPolicy(),
            new BasicScoringPolicies.BollingerPolicy(),
            new BasicScoringPolicies.AdxPolicy(),
            new BasicScoringPolicies.MacdPolicy()
    ));

    @Nested
    @DisplayName("종합 점수 산출 테스트")
    class CalculationTests {

        @Test
        @DisplayName("매수 신호 조합은 정책 점수를 합산해 100점으로 제한한다")
        void calculateScore_BuySignals_ClampsToMaximum() {
            TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                    AlignmentStatus.PERFECT,
                    BigDecimal.valueOf(25), // RSI < 30 (+15)
                    BigDecimal.valueOf(30), // ADX > 25 & PERFECT (+5)
                    null, null,
                    BigDecimal.valueOf(5), BigDecimal.valueOf(3), // MACD Golden (+10)
                    true, false, true, // isGoldenCross (+15), isMacdCross
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(90), // close <= bbLower (+10)
                    BigDecimal.valueOf(120)
            );
            // 기본 50 + Alignment 20 + RSI 15 + Event 15 + Bollinger 10 + ADX 5 + MACD 10 = 125 -> 100

            int score = technicalScoreService.calculateScore(snapshot);

            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("매도 신호 조합은 정책 점수를 합산해 0점으로 제한한다")
        void calculateScore_SellSignals_ClampsToMinimum() {
            TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                    AlignmentStatus.REVERSE, // -20
                    BigDecimal.valueOf(80), // -15
                    BigDecimal.valueOf(30), // -5
                    null, null,
                    BigDecimal.valueOf(3), BigDecimal.valueOf(5), // -10
                    false, true, true, // -15
                    BigDecimal.valueOf(130),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(130) // -10
            );
            // 기본 50 - 20 - 15 - 5 - 10 - 15 - 10 = -25 -> 0

            int score = technicalScoreService.calculateScore(snapshot);

            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("중립 지표는 기본 점수 50점을 유지한다")
        void calculateScore_NeutralSignals() {
            TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                    AlignmentStatus.MIXED,
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(20),
                    null, null,
                    BigDecimal.valueOf(5), BigDecimal.valueOf(5),
                    false, false, false,
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(110)
            );

            int score = technicalScoreService.calculateScore(snapshot);

            assertThat(score).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("데이터 변환 테스트")
    class MappingTests {

        @Test
        @DisplayName("IndicatorSnapshot.from은 TechnicalIndicators 필드를 정확히 변환한다")
        void indicatorSnapshotFrom_MapsTechnicalIndicators() {
            TechnicalIndicators indicators = new TechnicalIndicators(
                    BigDecimal.valueOf(5),
                    BigDecimal.valueOf(20),
                    BigDecimal.valueOf(60),
                    BigDecimal.valueOf(120),
                    BigDecimal.valueOf(29),
                    BigDecimal.valueOf(1.5),
                    BigDecimal.valueOf(1.2),
                    BigDecimal.valueOf(110),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(18),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(20),
                    AlignmentStatus.PERFECT,
                    true,
                    false,
                    true,
                    "상승 추세"
            );

            TechnicalScoreService.IndicatorSnapshot snapshot = TechnicalScoreService.IndicatorSnapshot.from(
                    indicators,
                    BigDecimal.valueOf(95)
            );

            assertThat(snapshot.alignment()).isEqualTo(AlignmentStatus.PERFECT);
            assertThat(snapshot.rsi()).isEqualByComparingTo("29");
            assertThat(snapshot.adx()).isEqualByComparingTo("18");
            assertThat(snapshot.plusDi()).isEqualByComparingTo("30");
            assertThat(snapshot.minusDi()).isEqualByComparingTo("20");
            assertThat(snapshot.macd()).isEqualByComparingTo("1.5");
            assertThat(snapshot.macdSignal()).isEqualByComparingTo("1.2");
            assertThat(snapshot.isGoldenCross()).isTrue();
            assertThat(snapshot.isDeadCross()).isFalse();
            assertThat(snapshot.isMacdCross()).isTrue();
            assertThat(snapshot.closePrice()).isEqualByComparingTo("95");
            assertThat(snapshot.bbLower()).isEqualByComparingTo("90");
            assertThat(snapshot.bbUpper()).isEqualByComparingTo("110");
        }
    }
}
