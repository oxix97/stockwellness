package org.stockwellness.adapter.out.persistence.stock;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.SectorLeadingStockCandidate;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;

@Component
@RequiredArgsConstructor
public class SectorLeadingStockAdapter implements SectorLeadingStockPort {
    private final JdbcTemplate jdbc;

    @Override
    public List<SectorLeadingStockCandidate> finalCandidates(long seriesId, LocalDate tradeDate) {
        if (seriesId <= 0 || tradeDate == null) {
            throw new IllegalArgumentException("Index series and trade date are required");
        }
        return jdbc.query("""
                WITH final_prices AS (
                    SELECT p.stock_id, p.trade_date, p.quote_scope, p.close_price,
                           p.trading_amount, p.revision,
                           LAG(p.close_price) OVER (PARTITION BY p.stock_id, p.quote_scope ORDER BY p.trade_date) AS previous_close
                    FROM index_series series
                    JOIN market_index idx ON idx.id = series.index_id AND idx.kind = 'SECTOR'
                    JOIN index_daily day ON day.series_id = series.id AND day.trade_date = ?
                        AND day.price_status = 'FINAL' AND day.close_value IS NOT NULL
                    JOIN stock s ON s.sector_index_id = idx.id
                    JOIN stock_price_daily p ON p.stock_id = s.id AND p.quote_scope = 'KRX'
                        AND p.trade_date <= ? AND p.data_status = 'FINAL'
                        AND p.close_price IS NOT NULL AND p.trading_amount IS NOT NULL
                        AND p.trading_amount >= 0
                    WHERE series.id = ?
                )
                SELECT stock_id, trade_date, quote_scope, previous_close, close_price, trading_amount,
                       revision AS price_revision,
                       (SELECT revision FROM index_daily WHERE series_id = ? AND trade_date = ?) AS index_revision
                FROM final_prices
                WHERE trade_date = ? AND previous_close > 0
                """, (rs, rowNum) -> new SectorLeadingStockCandidate(
                rs.getLong("stock_id"), rs.getObject("trade_date", LocalDate.class), rs.getString("quote_scope"),
                rs.getBigDecimal("previous_close"), rs.getBigDecimal("close_price"),
                rs.getBigDecimal("trading_amount"), rs.getInt("price_revision"), rs.getInt("index_revision")),
                tradeDate, tradeDate, seriesId, seriesId, tradeDate, tradeDate);
    }

    @Override
    public List<Long> seriesIdsForStock(long stockId) {
        if (stockId <= 0) throw new IllegalArgumentException("Stock id must be positive");
        return jdbc.query("""
                SELECT series.id
                FROM stock
                JOIN market_index sector ON sector.id = stock.sector_index_id AND sector.kind = 'SECTOR'
                JOIN index_series series ON series.index_id = sector.id
                WHERE stock.id = ? AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                ORDER BY series.id
                """, (rs, rowNum) -> rs.getLong("id"), stockId);
    }

    @Override
    @Transactional
    public void replace(long seriesId, LocalDate tradeDate, List<RankedSectorStock> stocks, String inputManifest) {
        if (seriesId <= 0 || tradeDate == null || stocks == null
                || inputManifest == null || inputManifest.isBlank()) {
            throw new IllegalArgumentException("A leading stock result and its manifest are required");
        }
        if (jdbc.queryForObject("""
                SELECT COUNT(*) FROM index_daily
                WHERE series_id = ? AND trade_date = ? AND price_status = 'FINAL' AND close_value IS NOT NULL
                """, Integer.class, seriesId, tradeDate) != 1) {
            return;
        }
        for (int index = 0; index < stocks.size(); index++) {
            RankedSectorStock stock = stocks.get(index);
            if (stock == null || stock.stockId() <= 0 || !tradeDate.equals(stock.priceDate())
                    || !"KRX".equals(stock.quoteScope()) || stock.rank() != index + 1 || stock.rank() > 5) {
                throw new IllegalArgumentException("Leading stock ranks and source prices are invalid");
            }
        }

        Timestamp createdAt = Timestamp.from(Instant.now());
        jdbc.update("""
                UPDATE market_signal SET input_manifest = input_manifest || '{"stale":true}'::jsonb
                WHERE signal_type = 'SECTOR_LEADING_STOCK'
                  AND rule_version = 'sector-leading-turnover-gainers-v1'
                  AND trade_date = ? AND quote_scope = 'KRX'
                  AND stock_id IN (SELECT stock_id FROM sector_leading_stock
                                   WHERE series_id = ? AND trade_date = ?)
                """, tradeDate, seriesId, tradeDate);
        jdbc.update("DELETE FROM sector_leading_stock WHERE series_id = ? AND trade_date = ?", seriesId, tradeDate);
        for (RankedSectorStock stock : stocks) {
            jdbc.update("""
                    INSERT INTO market_signal(stock_id, trade_date, quote_scope, signal_type, rule_version,
                                              input_manifest, created_at)
                    VALUES (?, ?, ?, 'SECTOR_LEADING_STOCK', 'sector-leading-turnover-gainers-v1', ?::jsonb, ?)
                    ON CONFLICT (stock_id, trade_date, quote_scope, signal_type, rule_version)
                    DO UPDATE SET input_manifest = EXCLUDED.input_manifest
                    WHERE market_signal.input_manifest IS DISTINCT FROM EXCLUDED.input_manifest
                    """, stock.stockId(), tradeDate, stock.quoteScope(), inputManifest, createdAt);
        }
        if (stocks.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO sector_leading_stock(series_id, trade_date, rank, stock_id, price_date, quote_scope)
                VALUES (?, ?, ?, ?, ?, ?)
                """, stocks, stocks.size(), (ps, stock) -> {
            ps.setLong(1, seriesId);
            ps.setObject(2, tradeDate);
            ps.setInt(3, stock.rank());
            ps.setLong(4, stock.stockId());
            ps.setObject(5, stock.priceDate());
            ps.setString(6, stock.quoteScope());
        });
    }
}
