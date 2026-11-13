package org.stockwellness.adapter.out.persistence.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.StockAiAnalysisPort;
import org.stockwellness.application.port.out.stock.StockAiAnalysisPort.StockAiAnalysisRecord;

@Component
@RequiredArgsConstructor
public class StockAiAnalysisAdapter implements StockAiAnalysisPort {
    private static final String UPDATE_EXISTING = """
            UPDATE stock_ai_analysis
               SET model_version = ?, prompt_version = ?, result = ?::jsonb, status = ?, generated_at = ?
             WHERE stock_id = ? AND trade_date = ? AND quote_scope = ? AND analysis_kind = ?
               AND input_manifest = ?::jsonb
            """;
    private static final String INSERT = """
            INSERT INTO stock_ai_analysis
                (stock_id, trade_date, quote_scope, analysis_kind, model_version, prompt_version,
                 input_manifest, result, status, generated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(StockAiAnalysisRecord record) {
        String result = serialize(record.result());
        String lockKey = record.stockId() + "|" + record.tradeDate() + "|" + record.quoteScope()
                + "|" + record.analysisKind() + "|" + record.inputManifest();
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, lockKey);

        int updated = jdbc.update(UPDATE_EXISTING,
                record.modelVersion(), record.promptVersion(), result, record.status(),
                java.sql.Timestamp.from(record.generatedAt()), record.stockId(),
                java.sql.Date.valueOf(record.tradeDate()), record.quoteScope(), record.analysisKind(), record.inputManifest());
        if (updated == 0) {
            jdbc.update(INSERT,
                    record.stockId(), java.sql.Date.valueOf(record.tradeDate()), record.quoteScope(), record.analysisKind(),
                    record.modelVersion(), record.promptVersion(), record.inputManifest(), result, record.status(),
                    java.sql.Timestamp.from(record.generatedAt()));
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize stock AI analysis result", exception);
        }
    }
}
