package org.stockwellness.domain.stock.analysis;

import java.util.List;

public record AiReport(
        InvestmentDecision decision, // BUY, SELL, HOLD
        int confidenceScore,         // 0 ~ 100
        String title,                // 한 줄 요약 (헤드라인)
        List<String> keyReasons,     // 핵심 근거 3가지
        String detailedAnalysis      // 상세 리포트 본문
) {

    public static AiReport fallback() {
        return new AiReport(
                InvestmentDecision.HOLD,
                0,
                "분석 시스템 일시 장애",
                List.of("AI 응답을 처리하는 중 오류가 발생했습니다."),
                "잠시 후 다시 시도해 주세요. 지속적으로 발생할 경우 고객센터로 문의 바랍니다."
        );
    }
}