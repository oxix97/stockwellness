package org.stockwellness.application.port.out.stock;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

public interface LoadTechnicalDataPort {
    /**
     * 종목 코드에 해당하는 AI 분석용 기술적 데이터 컨텍스트를 로드합니다.
     */
    CanonicalStockAnalysisInput loadTechnicalContext(String ticker);

    /**
     * 여러 종목 코드에 해당하는 AI 분석용 기술적 데이터 컨텍스트를 배치로 로드합니다.
     */
    Map<String, CanonicalStockAnalysisInput> loadTechnicalContexts(List<String> tickers);

    record CanonicalStockAnalysisInput(
            long stockId,
            String ticker,
            String stockName,
            LocalDate tradeDate,
            String quoteScope,
            int priceRevision,
            String inputManifest,
            String indicatorAlgorithmVersion,
            AiAnalysisContext context
    ) {
        public CanonicalStockAnalysisInput {
            if (stockId <= 0 || ticker == null || ticker.isBlank() || stockName == null || stockName.isBlank() || tradeDate == null
                    || quoteScope == null || quoteScope.isBlank() || priceRevision < 1
                    || inputManifest == null || inputManifest.isBlank()
                    || indicatorAlgorithmVersion == null || indicatorAlgorithmVersion.isBlank()
                    || context == null || !ticker.equals(context.isinCode()) || !tradeDate.equals(context.baseDate())) {
                throw new IllegalArgumentException("Canonical FINAL stock analysis input is required");
            }
        }
    }
}
