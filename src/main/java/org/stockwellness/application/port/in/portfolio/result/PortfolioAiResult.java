package org.stockwellness.application.port.in.portfolio.result;

import java.util.List;

public record PortfolioAiResult(
        String summary,    // e.g., "Growth Archer"
        String insight,    // Overall commentary
        List<String> nextSteps // Actionable advice
) {
    public static PortfolioAiResult fallback() {
        return new PortfolioAiResult(
                "진단 일시 장애",
                "AI 인사이트를 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도 해주세요.",
                List.of("데이터를 다시 확인해주세요.", "시스템 관리자에게 문의하세요.")
        );
    }
}
