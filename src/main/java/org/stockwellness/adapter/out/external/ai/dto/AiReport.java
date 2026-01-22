package org.stockwellness.adapter.out.external.ai.dto;

import java.util.List;

public record AiReport(
        InvestmentDecision decision, // BUY, SELL, HOLD
        int confidenceScore,         // 0 ~ 100
        String title,                // 한 줄 요약 (헤드라인)
        List<String> keyReasons,     // 핵심 근거 3가지
        String detailedAnalysis      // 상세 리포트 본문
) {
    public enum InvestmentDecision {
        BUY, SELL, HOLD
    }
}