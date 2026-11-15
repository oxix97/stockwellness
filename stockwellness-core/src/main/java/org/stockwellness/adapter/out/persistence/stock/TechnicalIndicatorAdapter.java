package org.stockwellness.adapter.out.persistence.stock;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.TechnicalIndicatorPort;
import org.stockwellness.domain.stock.insight.IndexTechnicalIndicator;
import org.stockwellness.domain.stock.price.StockTechnicalIndicator;

@Component
@RequiredArgsConstructor
public class TechnicalIndicatorAdapter implements TechnicalIndicatorPort {
    private static final String UPSERT_STOCK = """
            INSERT INTO stock_technical_indicator AS existing
                (stock_id, trade_date, quote_scope, price_basis, ma5, ma20, ma20_disparity, ma60, ma120,
                 rsi14, macd, macd_signal, adx, plus_di, minus_di, input_manifest, algorithm_version, calculated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (stock_id, trade_date, quote_scope, price_basis) DO UPDATE SET
                ma5 = EXCLUDED.ma5, ma20 = EXCLUDED.ma20, ma20_disparity = EXCLUDED.ma20_disparity,
                ma60 = EXCLUDED.ma60, ma120 = EXCLUDED.ma120, rsi14 = EXCLUDED.rsi14,
                macd = EXCLUDED.macd, macd_signal = EXCLUDED.macd_signal, adx = EXCLUDED.adx,
                plus_di = EXCLUDED.plus_di, minus_di = EXCLUDED.minus_di,
                input_manifest = EXCLUDED.input_manifest, algorithm_version = EXCLUDED.algorithm_version,
                calculated_at = EXCLUDED.calculated_at
            WHERE (existing.ma5, existing.ma20, existing.ma20_disparity, existing.ma60, existing.ma120,
                   existing.rsi14, existing.macd, existing.macd_signal, existing.adx, existing.plus_di,
                   existing.minus_di, existing.input_manifest, existing.algorithm_version)
                  IS DISTINCT FROM
                  (EXCLUDED.ma5, EXCLUDED.ma20, EXCLUDED.ma20_disparity, EXCLUDED.ma60, EXCLUDED.ma120,
                   EXCLUDED.rsi14, EXCLUDED.macd, EXCLUDED.macd_signal, EXCLUDED.adx, EXCLUDED.plus_di,
                   EXCLUDED.minus_di, EXCLUDED.input_manifest, EXCLUDED.algorithm_version)
            """;

    private static final String UPSERT_INDEX = """
            INSERT INTO index_technical_indicator AS existing
                (series_id, trade_date, ma5, ma20, ma20_disparity, ma60, ma120, rsi14, macd, macd_signal,
                 adx, plus_di, minus_di, input_manifest, algorithm_version, calculated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (series_id, trade_date) DO UPDATE SET
                ma5 = EXCLUDED.ma5, ma20 = EXCLUDED.ma20, ma20_disparity = EXCLUDED.ma20_disparity,
                ma60 = EXCLUDED.ma60, ma120 = EXCLUDED.ma120, rsi14 = EXCLUDED.rsi14,
                macd = EXCLUDED.macd, macd_signal = EXCLUDED.macd_signal, adx = EXCLUDED.adx,
                plus_di = EXCLUDED.plus_di, minus_di = EXCLUDED.minus_di,
                input_manifest = EXCLUDED.input_manifest, algorithm_version = EXCLUDED.algorithm_version,
                calculated_at = EXCLUDED.calculated_at
            WHERE (existing.ma5, existing.ma20, existing.ma20_disparity, existing.ma60, existing.ma120,
                   existing.rsi14, existing.macd, existing.macd_signal, existing.adx, existing.plus_di,
                   existing.minus_di, existing.input_manifest, existing.algorithm_version)
                  IS DISTINCT FROM
                  (EXCLUDED.ma5, EXCLUDED.ma20, EXCLUDED.ma20_disparity, EXCLUDED.ma60, EXCLUDED.ma120,
                   EXCLUDED.rsi14, EXCLUDED.macd, EXCLUDED.macd_signal, EXCLUDED.adx, EXCLUDED.plus_di,
                   EXCLUDED.minus_di, EXCLUDED.input_manifest, EXCLUDED.algorithm_version)
            """;

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void upsertStock(StockTechnicalIndicator value) {
        jdbc.update(UPSERT_STOCK, ps -> setStock(ps, value));
    }

    @Override
    @Transactional
    public void upsertIndex(IndexTechnicalIndicator value) {
        jdbc.update(UPSERT_INDEX, ps -> setIndex(ps, value));
    }

    private static void setStock(PreparedStatement ps, StockTechnicalIndicator value) throws SQLException {
        int i = 1;
        ps.setLong(i++, value.stockId());
        ps.setDate(i++, java.sql.Date.valueOf(value.tradeDate()));
        ps.setString(i++, value.quoteScope());
        ps.setString(i++, value.priceBasis().name());
        setValues(ps, i, value.ma5(), value.ma20(), value.ma20Disparity(), value.ma60(), value.ma120(),
                value.rsi14(), value.macd(), value.macdSignal(), value.adx(), value.plusDi(), value.minusDi(),
                value.inputManifest(), value.algorithmVersion(), value.calculatedAt());
    }

    private static void setIndex(PreparedStatement ps, IndexTechnicalIndicator value) throws SQLException {
        int i = 1;
        ps.setLong(i++, value.seriesId());
        ps.setDate(i++, java.sql.Date.valueOf(value.tradeDate()));
        setValues(ps, i, value.ma5(), value.ma20(), value.ma20Disparity(), value.ma60(), value.ma120(),
                value.rsi14(), value.macd(), value.macdSignal(), value.adx(), value.plusDi(), value.minusDi(),
                value.inputManifest(), value.algorithmVersion(), value.calculatedAt());
    }

    private static void setValues(PreparedStatement ps, int index, BigDecimal ma5, BigDecimal ma20,
            BigDecimal disparity, BigDecimal ma60, BigDecimal ma120,
            BigDecimal rsi14, BigDecimal macd, BigDecimal macdSignal,
            BigDecimal adx, BigDecimal plusDi, BigDecimal minusDi,
            String inputManifest, String algorithmVersion, Instant calculatedAt) throws SQLException {
        ps.setBigDecimal(index++, ma5);
        ps.setBigDecimal(index++, ma20);
        ps.setBigDecimal(index++, disparity);
        ps.setBigDecimal(index++, ma60);
        ps.setBigDecimal(index++, ma120);
        ps.setBigDecimal(index++, rsi14);
        ps.setBigDecimal(index++, macd);
        ps.setBigDecimal(index++, macdSignal);
        ps.setBigDecimal(index++, adx);
        ps.setBigDecimal(index++, plusDi);
        ps.setBigDecimal(index++, minusDi);
        ps.setString(index++, inputManifest);
        ps.setString(index++, algorithmVersion);
        ps.setTimestamp(index, Timestamp.from(calculatedAt));
    }
}
