package org.stockwellness.domain.stock.analysis;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * 기본 기술적 점수 산출 정책 모음
 */
public class BasicScoringPolicies {

    @Component
    public static class AlignmentPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            return switch (snapshot.alignment()) {
                case PERFECT -> 20;
                case REVERSE -> -20;
                case MIXED -> 0;
                case null -> 0;
            };
        }
    }

    @Component
    public static class RsiPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            BigDecimal rsi = snapshot.rsi();
            if (rsi == null) return 0;
            return switch (rsi) {
                case BigDecimal r when r.compareTo(BigDecimal.valueOf(30)) < 0 -> 15;
                case BigDecimal r when r.compareTo(BigDecimal.valueOf(70)) > 0 -> -15;
                default -> 0;
            };
        }
    }

    @Component
    public static class EventPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            int score = 0;
            if (snapshot.isGoldenCross()) score += 15;
            if (snapshot.isDeadCross()) score -= 15;
            return score;
        }
    }

    @Component
    public static class BollingerPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            if (snapshot.closePrice() == null || snapshot.bbLower() == null || snapshot.bbUpper() == null) {
                return 0;
            }
            if (snapshot.closePrice().compareTo(snapshot.bbLower()) <= 0) return 10;
            if (snapshot.closePrice().compareTo(snapshot.bbUpper()) >= 0) return -10;
            return 0;
        }
    }

    @Component
    public static class AdxPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            if (snapshot.adx() == null || snapshot.adx().compareTo(BigDecimal.valueOf(25)) <= 0) {
                return 0;
            }
            if (snapshot.alignment() == null) return 0;
            return switch (snapshot.alignment()) {
                case PERFECT -> 5;
                case REVERSE -> -5;
                default -> 0;
            };
        }
    }

    @Component
    public static class MacdPolicy implements ScoringPolicy {
        @Override
        public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
            if (!snapshot.isMacdCross() || snapshot.macd() == null || snapshot.macdSignal() == null) {
                return 0;
            }
            return snapshot.macd().compareTo(snapshot.macdSignal()) > 0 ? 10 : -10;
        }
    }
}
