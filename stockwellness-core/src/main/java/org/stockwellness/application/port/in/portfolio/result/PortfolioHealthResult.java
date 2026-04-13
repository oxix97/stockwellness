package org.stockwellness.application.port.in.portfolio.result;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record PortfolioHealthResult(
        int overallScore,
        Map<String, Integer> categories,
        List<StockContributionResult> stockContributions,
        BigDecimal mdd,
        BigDecimal relativeMdd,
        BigDecimal sharpeRatio,
        BigDecimal alpha,
        String summary,
        String insight,
        List<String> nextSteps
) {
    public static PortfolioHealthResult mock() {
        return new PortfolioHealthResult(
                80,
                Map.of("Diversification", 85, "RiskAdjustment", 75, "Stability", 80, "Efficiency", 85, "Momentum", 75),
                Collections.emptyList(),
                new BigDecimal("-0.15"),
                new BigDecimal("-0.05"),
                new BigDecimal("1.2"),
                new BigDecimal("0.05"),
                "Mock AI 진단 결과입니다. (비용 절감을 위해 실제 AI 호출이 비활성화되었습니다.)",
                "포트폴리오가 전반적으로 안정적입니다.",
                List.of("분산 투자를 유지하세요.", "MDD 관리에 유의하세요.")
        );
    }
}