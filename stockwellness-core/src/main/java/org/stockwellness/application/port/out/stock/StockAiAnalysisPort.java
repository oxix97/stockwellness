package org.stockwellness.application.port.out.stock;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;

public interface StockAiAnalysisPort {
    void save(StockAiAnalysisRecord record);

    record StockAiAnalysisRecord(
            long stockId,
            LocalDate tradeDate,
            String quoteScope,
            String analysisKind,
            String modelVersion,
            String promptVersion,
            String inputManifest,
            StockAnalysisResult result,
            String status,
            Instant generatedAt
    ) {
        public StockAiAnalysisRecord {
            if (stockId <= 0 || tradeDate == null || quoteScope == null || quoteScope.isBlank()
                    || analysisKind == null || analysisKind.isBlank() || analysisKind.length() > 40
                    || modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 100
                    || promptVersion == null || promptVersion.isBlank() || promptVersion.length() > 100
                    || inputManifest == null || inputManifest.isBlank() || result == null
                    || !Set.of("SUCCEEDED", "FAILED").contains(status) || generatedAt == null) {
                throw new IllegalArgumentException("Stock AI analysis persistence data is required");
            }
        }
    }
}
