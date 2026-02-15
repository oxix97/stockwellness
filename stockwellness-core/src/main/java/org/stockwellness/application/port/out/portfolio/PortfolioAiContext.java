package org.stockwellness.application.port.out.portfolio;

import java.util.Map;

/**
 * AI 분석을 위해 필요한 포트폴리오 진단 데이터 컨텍스트
 */
public record PortfolioAiContext(
        int overallScore,
        Map<String, Integer> categories
) {
}
