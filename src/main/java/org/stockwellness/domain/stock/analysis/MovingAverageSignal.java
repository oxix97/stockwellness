package org.stockwellness.domain.stock.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.stock.StockHistory;

@Getter
@RequiredArgsConstructor
public enum MovingAverageSignal {
    GOLDEN_CROSS("★ 핵심 신호: 골든크로스 발생 (5일선이 20일선을 상향 돌파)"),
    DEAD_CROSS("⚠️ 위험 신호: 데드크로스 발생 (5일선이 20일선을 하향 이탈)"),
    BULLISH_ALIGN("정배열 (단기 상승 추세)"),
    BEARISH_ALIGN("역배열 (단기 하락 추세)");

    private final String description;

    public static String analyze(StockHistory today, StockHistory yesterday) {
        if (today.getMa5() == null || today.getMa20() == null) return "데이터 부족";

        boolean isBullish = today.getMa5().compareTo(today.getMa20()) > 0;
        String baseStatus = isBullish ? BULLISH_ALIGN.description : BEARISH_ALIGN.description;

        // 크로스오버 체크
        if (yesterday != null && yesterday.getMa5() != null && yesterday.getMa20() != null) {
            boolean wasBullish = yesterday.getMa5().compareTo(yesterday.getMa20()) > 0;
            
            if (!wasBullish && isBullish) return baseStatus + "\n   - " + GOLDEN_CROSS.description;
            if (wasBullish && !isBullish) return baseStatus + "\n   - " + DEAD_CROSS.description;
        }
        
        return baseStatus;
    }
}