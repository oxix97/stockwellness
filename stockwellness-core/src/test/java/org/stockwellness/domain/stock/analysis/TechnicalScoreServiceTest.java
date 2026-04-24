package org.stockwellness.domain.stock.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TechnicalScoreService 단위 테스트")
class TechnicalScoreServiceTest {

    private final TechnicalScoreService technicalScoreService = new TechnicalScoreService(List.of(
            new BasicScoringPolicies.AlignmentPolicy(),
            new BasicScoringPolicies.RsiPolicy(),
            new BasicScoringPolicies.EventPolicy(),
            new BasicScoringPolicies.BollingerPolicy()
    ));

    @Test
    @DisplayName("매수 신호 조합은 정책 점수를 합산해 100점으로 제한한다")
    void calculateScore_BuySignals_ClampsToMaximum() {
        TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                AlignmentStatus.PERFECT,
                BigDecimal.ZERO,
                BigDecimal.valueOf(25),
                true,
                false,
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(120)
        );

        int score = technicalScoreService.calculateScore(snapshot);

        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("매도 신호 조합은 정책 점수를 합산해 0점으로 제한한다")
    void calculateScore_SellSignals_ClampsToMinimum() {
        TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                AlignmentStatus.REVERSE,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(25),
                false,
                true,
                BigDecimal.valueOf(130),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(130)
        );

        int score = technicalScoreService.calculateScore(snapshot);

        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("RSI 경계값 0과 100은 각각 과매도와 과매수 점수로 반영한다")
    void calculateScore_RsiExtremeBoundaries() {
        TechnicalScoreService onlyRsiService = new TechnicalScoreService(List.of(new BasicScoringPolicies.RsiPolicy()));
        TechnicalScoreService.IndicatorSnapshot oversold = new TechnicalScoreService.IndicatorSnapshot(
                AlignmentStatus.MIXED,
                BigDecimal.ZERO,
                null,
                false,
                false,
                null,
                null,
                null
        );
        TechnicalScoreService.IndicatorSnapshot overbought = new TechnicalScoreService.IndicatorSnapshot(
                AlignmentStatus.MIXED,
                BigDecimal.valueOf(100),
                null,
                false,
                false,
                null,
                null,
                null
        );

        assertThat(onlyRsiService.calculateScore(oversold)).isEqualTo(65);
        assertThat(onlyRsiService.calculateScore(overbought)).isEqualTo(35);
    }

    @Test
    @DisplayName("중립 지표는 기본 점수 50점을 유지한다")
    void calculateScore_NeutralSignals() {
        TechnicalScoreService.IndicatorSnapshot snapshot = new TechnicalScoreService.IndicatorSnapshot(
                AlignmentStatus.MIXED,
                BigDecimal.valueOf(50),
                null,
                false,
                false,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(110)
        );

        int score = technicalScoreService.calculateScore(snapshot);

        assertThat(score).isEqualTo(50);
    }

    @Test
    @DisplayName("IndicatorSnapshot.from은 TechnicalIndicators 필드를 null-safe하게 변환한다")
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
                null,
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
        assertThat(snapshot.isGoldenCross()).isTrue();
        assertThat(snapshot.isDeadCross()).isFalse();
        assertThat(snapshot.closePrice()).isEqualByComparingTo("95");
        assertThat(snapshot.bbLower()).isEqualByComparingTo("90");
        assertThat(snapshot.bbUpper()).isEqualByComparingTo("110");
    }
}
