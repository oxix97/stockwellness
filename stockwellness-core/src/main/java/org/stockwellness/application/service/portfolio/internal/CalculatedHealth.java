package org.stockwellness.application.service.portfolio.internal;

import org.stockwellness.application.port.in.portfolio.result.StockContributionResult;

import java.util.List;
import java.util.Map;

/**
 * 계산된 건강 상태 점수 결과
 */
public record CalculatedHealth(
        int overallScore,
        Map<String, Integer> categories,
        List<StockContributionResult> stockContributions
) {
}
