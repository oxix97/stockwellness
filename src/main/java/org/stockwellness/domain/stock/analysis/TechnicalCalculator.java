package org.stockwellness.domain.stock.analysis;

import java.math.BigDecimal;

public class TechnicalCalculator {

    /**
     * 이동평균선 분석 로직 (BigDecimal 전용)
     * @param c5   현재 MA 5
     * @param c20  현재 MA 20
     * @param c60  현재 MA 60
     * @param c120 현재 MA 120
     * @param p5   어제 MA 5 (크로스 감지용)
     * @param p20  어제 MA 20 (크로스 감지용)
     */
    public static MarketCondition analyze(
            BigDecimal c5, BigDecimal c20, BigDecimal c60, BigDecimal c120,
            BigDecimal p5, BigDecimal p20
    ) {
        // null 방어 로직 (데이터가 부족한 초기 상장 주식 등)
        if (c5 == null || c20 == null || c60 == null || c120 == null) {
            return new MarketCondition(TrendStatus.NEUTRAL, CrossoverSignal.NONE, "데이터 부족");
        }

        // 1. 추세 상태 (Trend Status) 판별
        TrendStatus trendStatus = TrendStatus.NEUTRAL;

        // 정배열: 5 > 20 > 60 > 120
        if (gt(c5, c20) && gt(c20, c60) && gt(c60, c120)) {
            trendStatus = TrendStatus.REGULAR;
        }
        // 역배열: 5 < 20 < 60 < 120
        else if (lt(c5, c20) && lt(c20, c60) && lt(c60, c120)) {
            trendStatus = TrendStatus.INVERSE;
        }

        // 2. 크로스오버 신호 (Signal) 감지
        CrossoverSignal signal = CrossoverSignal.NONE;

        // 어제 데이터가 있을 때만 계산
        if (p5 != null && p20 != null) {
            boolean isBullishNow = gt(c5, c20);   // 오늘 5 > 20
            boolean wasBullishPrev = gt(p5, p20); // 어제 5 > 20

            if (!wasBullishPrev && isBullishNow) {
                signal = CrossoverSignal.GOLDEN_CROSS; // 어제는 아래, 오늘은 위 (상향돌파)
            } else if (wasBullishPrev && !isBullishNow) {
                signal = CrossoverSignal.DEAD_CROSS;   // 어제는 위, 오늘은 아래 (하향이탈)
            }
        }

        // 3. 결과 반환
        String desc = String.format("추세: %s, 신호: %s", trendStatus.getDescription(), signal);
        return new MarketCondition(trendStatus, signal, desc);
    }

    // --- Helper Methods (가독성 향상) ---

    // a > b
    private static boolean gt(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    // a < b
    private static boolean lt(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) < 0;
    }
}