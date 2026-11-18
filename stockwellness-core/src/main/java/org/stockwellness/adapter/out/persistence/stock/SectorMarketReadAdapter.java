package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorMarketObservation;
import org.stockwellness.application.port.out.stock.SectorMarketReadPort;
import org.stockwellness.domain.stock.insight.LeadingStock;

@Component
@RequiredArgsConstructor
public class SectorMarketReadAdapter implements SectorMarketReadPort {
    private static final String OBSERVATION_SQL = """
            WITH final_prices AS (
                SELECT daily.series_id, daily.trade_date, daily.close_value, daily.revision,
                       LAG(daily.close_value) OVER (PARTITION BY daily.series_id ORDER BY daily.trade_date) AS previous_close
                FROM index_daily daily
                WHERE daily.price_status = 'FINAL' AND daily.trade_date <= ?
            )
            SELECT series.id AS series_id, split_part(market.index_key, ':', 2) AS sector_code,
                   market.name AS sector_name, series.calendar_market_code AS market_code,
                   price.trade_date, price.close_value AS close_price,
                   CASE WHEN price.previous_close > 0
                        THEN (price.close_value - price.previous_close) / price.previous_close * 100
                   END AS change_rate,
                   indicator.ma5, indicator.ma20, indicator.rsi14
            FROM final_prices price
            JOIN index_series series ON series.id = price.series_id
            JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
            JOIN market_index market ON market.id = series.index_id AND market.kind = 'SECTOR'
            LEFT JOIN index_technical_indicator indicator
              ON indicator.series_id = price.series_id AND indicator.trade_date = price.trade_date
             AND indicator.input_manifest ->> 'source' = 'index_daily'
             AND indicator.input_manifest ->> 'sourceId' = series.id::text
             AND indicator.input_manifest ->> 'targetDate' = price.trade_date::text
             AND jsonb_typeof(indicator.input_manifest -> 'observations') = 'array'
             AND EXISTS (
                 SELECT 1 FROM jsonb_array_elements(indicator.input_manifest -> 'observations') AS input(value)
                 WHERE input.value ->> 'date' = price.trade_date::text
             )
             AND NOT EXISTS (
                 SELECT 1 FROM jsonb_array_elements(indicator.input_manifest -> 'observations') AS input(value)
                 LEFT JOIN index_daily source_day
                   ON source_day.series_id = price.series_id
                  AND source_day.trade_date::text = input.value ->> 'date'
                 WHERE source_day.series_id IS NULL
                    OR source_day.price_status <> 'FINAL'
                    OR source_day.revision::text IS DISTINCT FROM input.value ->> 'revision'
             )
            WHERE series.is_active AND series.is_canonical
              AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
              AND price.close_value IS NOT NULL
            """;

    private final JdbcTemplate jdbc;

    @Override
    public Optional<LocalDate> latestFinalDate(String marketCode) {
        String marketFilter = marketCode == null ? "" : " AND series.calendar_market_code = ?";
        Object[] parameters = marketCode == null ? new Object[0] : new Object[]{marketCode};
        return Optional.ofNullable(jdbc.query("""
                SELECT MAX(daily.trade_date)
                FROM index_daily daily
                JOIN index_series series ON series.id = daily.series_id
                JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                JOIN market_index market ON market.id = series.index_id
                WHERE market.kind = 'SECTOR' AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                  AND daily.price_status = 'FINAL' AND daily.close_value IS NOT NULL%s
                """.formatted(marketFilter), rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, parameters));
    }

    @Override
    public List<SectorMarketObservation> finalOnDate(LocalDate tradeDate, String marketCode) {
        if (tradeDate == null) throw new IllegalArgumentException("Sector trade date is required");
        String marketFilter = marketCode == null ? "" : " AND series.calendar_market_code = ?";
        Object[] parameters = marketCode == null
                ? new Object[]{tradeDate, tradeDate}
                : new Object[]{tradeDate, tradeDate, marketCode};
        return jdbc.query(OBSERVATION_SQL + " AND price.trade_date = ?" + marketFilter
                + " ORDER BY sector_name, series.calendar_market_code",
                (rs, rowNum) -> observation(rs), parameters);
    }

    @Override
    public Optional<SectorMarketObservation> latestFinal(String sectorCode, LocalDate asOf) {
        if (sectorCode == null || sectorCode.isBlank() || asOf == null) {
            throw new IllegalArgumentException("Sector code and as-of date are required");
        }
        return jdbc.query(OBSERVATION_SQL + " AND split_part(market.index_key, ':', 2) = ?"
                + " ORDER BY price.trade_date DESC, series.calendar_market_code LIMIT 1",
                (rs, rowNum) -> observation(rs), asOf, sectorCode).stream().findFirst();
    }

    @Override
    public List<SectorMarketObservation> historyFinal(long seriesId, LocalDate endDate, int limit) {
        if (seriesId <= 0 || endDate == null || limit < 1) {
            throw new IllegalArgumentException("Sector series, end date, and positive limit are required");
        }
        return jdbc.query(OBSERVATION_SQL + " AND series.id = ? ORDER BY price.trade_date DESC LIMIT ?",
                (rs, rowNum) -> observation(rs), endDate, seriesId, limit);
    }

    @Override
    public List<LeadingStock> leadingStocks(long seriesId, LocalDate tradeDate) {
        if (seriesId <= 0 || tradeDate == null) throw new IllegalArgumentException("Sector and trade date are required");
        return jdbc.query("""
                SELECT stock.name, stock.ticker, price.close_price, price.volume, price.trading_amount,
                       previous.close_price AS previous_close
                FROM sector_leading_stock leader
                JOIN index_series series ON series.id = leader.series_id
                JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                JOIN index_daily index_day ON index_day.series_id = leader.series_id
                    AND index_day.trade_date = leader.trade_date AND index_day.price_status = 'FINAL'
                JOIN market_signal signal ON signal.stock_id = leader.stock_id
                    AND signal.trade_date = leader.trade_date AND signal.quote_scope = leader.quote_scope
                    AND signal.signal_type = 'SECTOR_LEADING_STOCK'
                    AND signal.rule_version = 'sector-leading-turnover-gainers-v1'
                JOIN stock ON stock.id = leader.stock_id
                JOIN stock_price_daily price ON price.stock_id = leader.stock_id
                    AND price.trade_date = leader.price_date AND price.quote_scope = leader.quote_scope
                    AND price.data_status = 'FINAL'
                LEFT JOIN LATERAL (
                    SELECT prior.close_price
                    FROM stock_price_daily prior
                    WHERE prior.stock_id = price.stock_id AND prior.quote_scope = price.quote_scope
                      AND prior.trade_date < price.trade_date AND prior.data_status = 'FINAL'
                    ORDER BY prior.trade_date DESC LIMIT 1
                ) previous ON TRUE
                WHERE leader.series_id = ? AND leader.trade_date = ?
                  AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                  AND signal.input_manifest ->> 'source' = 'stock_price_daily'
                  AND signal.input_manifest ->> 'indexSeriesId' = leader.series_id::text
                  AND signal.input_manifest ->> 'tradeDate' = leader.trade_date::text
                  AND signal.input_manifest ->> 'stale' IS DISTINCT FROM 'true'
                  AND jsonb_typeof(signal.input_manifest -> 'candidates') = 'array'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM jsonb_array_elements(CASE
                          WHEN jsonb_typeof(signal.input_manifest -> 'candidates') = 'array'
                          THEN signal.input_manifest -> 'candidates' ELSE '[]'::jsonb END) AS input(value)
                      LEFT JOIN stock_price_daily source_price
                        ON source_price.stock_id::text = input.value ->> 'stockId'
                       AND source_price.trade_date::text = input.value ->> 'priceDate'
                       AND source_price.quote_scope = input.value ->> 'quoteScope'
                      LEFT JOIN index_daily source_index
                        ON source_index.series_id = leader.series_id
                       AND source_index.trade_date::text = input.value ->> 'priceDate'
                      WHERE source_price.stock_id IS NULL OR source_price.data_status <> 'FINAL'
                         OR source_price.revision::text IS DISTINCT FROM input.value ->> 'priceRevision'
                         OR source_index.series_id IS NULL OR source_index.price_status <> 'FINAL'
                         OR source_index.revision::text IS DISTINCT FROM input.value ->> 'indexRevision'
                  )
                ORDER BY leader.rank
                """, (rs, rowNum) -> new LeadingStock(rs.getString("name"), rs.getString("ticker"),
                changeRate(rs.getBigDecimal("close_price"), rs.getBigDecimal("previous_close")),
                rs.getObject("volume", Long.class), rs.getBigDecimal("trading_amount")), seriesId, tradeDate);
    }

    private static SectorMarketObservation observation(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SectorMarketObservation(rs.getLong("series_id"), rs.getString("sector_code"),
                rs.getString("sector_name"), rs.getString("market_code"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("close_price"),
                rs.getBigDecimal("change_rate"), rs.getBigDecimal("ma5"), rs.getBigDecimal("ma20"),
                rs.getBigDecimal("rsi14"));
    }

    private static BigDecimal changeRate(BigDecimal close, BigDecimal previousClose) {
        if (close == null || previousClose == null || previousClose.signum() <= 0) return null;
        return close.subtract(previousClose).divide(previousClose, 8, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
