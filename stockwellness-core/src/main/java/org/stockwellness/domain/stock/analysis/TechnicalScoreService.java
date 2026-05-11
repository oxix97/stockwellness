package org.stockwellness.domain.stock.analysis;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

/**
 * Java 21의 Record와 전략 패턴을 활용한 기술적 점수 산출 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalScoreService {

    private final List<ScoringPolicy> policies;

    /**
     * 지표 데이터를 담는 Java 21 Record
     */
    public record IndicatorSnapshot(
            AlignmentStatus alignment,
            BigDecimal rsi,
            BigDecimal adx,
            BigDecimal plusDi,
            BigDecimal minusDi,
            BigDecimal macd,
            BigDecimal macdSignal,
            boolean isGoldenCross,
            boolean isDeadCross,
            boolean isMacdCross,
            BigDecimal closePrice,
            BigDecimal bbLower,
            BigDecimal bbUpper
    ) {
        public static IndicatorSnapshot from(TechnicalIndicators ti, BigDecimal closePrice) {
            return new IndicatorSnapshot(
                    ti.getAlignmentStatus(),
                    ti.getRsi14(),
                    ti.getAdx(),
                    ti.getPlusDi(),
                    ti.getMinusDi(),
                    ti.getMacd(),
                    ti.getMacdSignal(),
                    Boolean.TRUE.equals(ti.getIsGoldenCross()),
                    Boolean.TRUE.equals(ti.getIsDeadCross()),
                    Boolean.TRUE.equals(ti.getIsMacdCross()),
                    closePrice,
                    ti.getBollingerLower(),
                    ti.getBollingerUpper()
            );
        }
    }

    /**
     * 종합 기술적 점수 산출
     */
    public int calculateScore(IndicatorSnapshot snapshot) {
        // 기본 점수 50에서 시작하여 주입된 모든 정책들의 결과 합산
        int totalScore = policies.stream()
                .mapToInt(policy -> policy.evaluate(snapshot))
                .sum();

        // 50점을 기준으로 상대적 점수 산출 (최종 0~100점 범위 제한)
        return Math.clamp(50 + totalScore, 0, 100);
    }
}
