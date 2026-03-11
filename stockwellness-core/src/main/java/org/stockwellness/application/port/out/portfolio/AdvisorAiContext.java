package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 리밸런싱 조언을 위해 필요한 데이터 컨텍스트
 */
public record AdvisorAiContext(
        String portfolioName,
        List<HoldingInfo> holdings,
        List<MarketBenchmark> benchmarks
) {
    public record HoldingInfo(
            String ticker,
            String name,
            BigDecimal quantity,
            BigDecimal currentPrice,
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            AiAnalysisContext technicalContext
    ) {}

    public record MarketBenchmark(
            String name,
            BigDecimal currentPrice,
            BigDecimal fluctuationRate
    ) {}
}
