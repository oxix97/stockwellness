package org.stockwellness.adapter.out.persistence.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;

@Component
@RequiredArgsConstructor
public class IndexDailyReadAdapter implements IndexDailyReadPort {
    private final JdbcTemplate jdbc;

    @Override
    public List<LocalDate> finalDatesFrom(long seriesId, LocalDate startDate, int limit) {
        if (seriesId <= 0 || startDate == null || limit < 1) {
            throw new IllegalArgumentException("Index series, start date, and positive limit are required");
        }
        return jdbc.query("""
                SELECT trade_date FROM index_daily
                WHERE series_id = ? AND trade_date >= ? AND price_status = 'FINAL' AND close_value IS NOT NULL
                ORDER BY trade_date LIMIT ?
                """, (rs, rowNum) -> rs.getObject("trade_date", LocalDate.class), seriesId, startDate, limit);
    }

    @Override
    public List<IndexPriceObservation> recentFinal(long seriesId, LocalDate asOf, int limit) {
        if (seriesId <= 0 || asOf == null || limit < 1) {
            throw new IllegalArgumentException("Index series, date, and positive limit are required");
        }
        return jdbc.query("""
                SELECT daily.series_id, daily.trade_date, daily.high_value, daily.low_value, daily.close_value,
                       daily.advance_count, daily.decline_count, daily.unchanged_count, daily.upper_limit_count,
                       daily.lower_limit_count, daily.revision, daily.price_status
                FROM index_daily daily
                JOIN index_series series ON series.id = daily.series_id
                JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                WHERE series.id = ? AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                  AND daily.trade_date <= ? AND daily.price_status = 'FINAL'
                  AND daily.high_value IS NOT NULL AND daily.low_value IS NOT NULL AND daily.close_value IS NOT NULL
                ORDER BY daily.trade_date DESC
                LIMIT ?
                """, (rs, rowNum) -> new IndexPriceObservation(rs.getLong("series_id"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("high_value"),
                rs.getBigDecimal("low_value"), rs.getBigDecimal("close_value"),
                rs.getObject("advance_count", Integer.class), rs.getObject("decline_count", Integer.class),
                rs.getObject("unchanged_count", Integer.class), rs.getObject("upper_limit_count", Integer.class),
                rs.getObject("lower_limit_count", Integer.class), rs.getInt("revision"),
                rs.getString("price_status")), seriesId, asOf, limit);
    }

    @Override
    public List<IndexPriceObservation> recentFinal(String publicBenchmarkCode, LocalDate asOf, int limit) {
        if (publicBenchmarkCode == null || publicBenchmarkCode.isBlank() || asOf == null || limit < 1) {
            throw new IllegalArgumentException("Benchmark code, date, and positive limit are required");
        }
        return jdbc.query("""
                SELECT series.id AS series_id, daily.trade_date, daily.high_value, daily.low_value,
                       daily.close_value, daily.advance_count, daily.decline_count, daily.unchanged_count,
                       daily.upper_limit_count, daily.lower_limit_count, daily.revision, daily.price_status
                FROM index_daily daily
                JOIN index_series series ON series.id = daily.series_id
                JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                JOIN market_index market_index ON market_index.id = series.index_id
                WHERE market_index.public_benchmark_code = ?
                  AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                  AND daily.trade_date <= ? AND daily.price_status = 'FINAL'
                  AND daily.close_value IS NOT NULL
                ORDER BY daily.trade_date DESC
                LIMIT ?
                """, (rs, rowNum) -> new IndexPriceObservation(rs.getLong("series_id"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("high_value"),
                rs.getBigDecimal("low_value"), rs.getBigDecimal("close_value"),
                rs.getObject("advance_count", Integer.class), rs.getObject("decline_count", Integer.class),
                rs.getObject("unchanged_count", Integer.class), rs.getObject("upper_limit_count", Integer.class),
                rs.getObject("lower_limit_count", Integer.class), rs.getInt("revision"),
                rs.getString("price_status")), publicBenchmarkCode, asOf, limit);
    }

    @Override
    public List<IndexPriceObservation> historyFinal(String publicBenchmarkCode, LocalDate start, LocalDate end) {
        if (publicBenchmarkCode == null || publicBenchmarkCode.isBlank() || start == null || end == null
                || start.isAfter(end)) {
            throw new IllegalArgumentException("Benchmark code and valid date range are required");
        }
        return jdbc.query("""
                WITH final_history AS (
                    SELECT series.id AS series_id, daily.trade_date, daily.open_value, daily.high_value,
                           daily.low_value, daily.close_value, daily.volume, daily.trading_amount,
                           daily.advance_count, daily.decline_count, daily.unchanged_count,
                           daily.upper_limit_count, daily.lower_limit_count, daily.revision, daily.price_status,
                           LAG(daily.close_value) OVER (PARTITION BY series.id ORDER BY daily.trade_date) AS previous_close
                    FROM index_daily daily
                    JOIN index_series series ON series.id = daily.series_id
                    JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                    JOIN market_index market_index ON market_index.id = series.index_id
                    WHERE market_index.public_benchmark_code = ?
                      AND series.is_active AND series.is_canonical
                      AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                      AND daily.trade_date <= ? AND daily.price_status = 'FINAL'
                )
                SELECT * FROM final_history WHERE trade_date >= ?
                ORDER BY trade_date ASC
                """, (rs, rowNum) -> new IndexPriceObservation(rs.getLong("series_id"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("high_value"),
                rs.getBigDecimal("low_value"), rs.getBigDecimal("close_value"),
                rs.getObject("advance_count", Integer.class), rs.getObject("decline_count", Integer.class),
                rs.getObject("unchanged_count", Integer.class), rs.getObject("upper_limit_count", Integer.class),
                rs.getObject("lower_limit_count", Integer.class), rs.getInt("revision"),
                rs.getString("price_status"), rs.getBigDecimal("open_value"), rs.getObject("volume", Long.class),
                rs.getBigDecimal("trading_amount"), rs.getBigDecimal("previous_close")),
                publicBenchmarkCode, end, start);
    }

    @Override
    public Optional<IndexPriceObservation> latestFinal(String publicBenchmarkCode, LocalDate asOf) {
        if (publicBenchmarkCode == null || publicBenchmarkCode.isBlank() || asOf == null) {
            throw new IllegalArgumentException("Benchmark code and as-of date are required");
        }
        return jdbc.query("""
                WITH final_history AS (
                    SELECT series.id AS series_id, daily.trade_date, daily.open_value, daily.high_value,
                           daily.low_value, daily.close_value, daily.volume, daily.trading_amount,
                           daily.advance_count, daily.decline_count, daily.unchanged_count,
                           daily.upper_limit_count, daily.lower_limit_count, daily.revision, daily.price_status,
                           LAG(daily.close_value) OVER (PARTITION BY series.id ORDER BY daily.trade_date) AS previous_close
                    FROM index_daily daily
                    JOIN index_series series ON series.id = daily.series_id
                    JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                    JOIN market_index market ON market.id = series.index_id
                    WHERE market.public_benchmark_code = ?
                      AND series.is_active AND series.is_canonical
                      AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                      AND daily.trade_date <= ? AND daily.price_status = 'FINAL'
                )
                SELECT * FROM final_history ORDER BY trade_date DESC LIMIT 1
                """, (rs, rowNum) -> new IndexPriceObservation(rs.getLong("series_id"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("high_value"),
                rs.getBigDecimal("low_value"), rs.getBigDecimal("close_value"),
                rs.getObject("advance_count", Integer.class), rs.getObject("decline_count", Integer.class),
                rs.getObject("unchanged_count", Integer.class), rs.getObject("upper_limit_count", Integer.class),
                rs.getObject("lower_limit_count", Integer.class), rs.getInt("revision"),
                rs.getString("price_status"), rs.getBigDecimal("open_value"), rs.getObject("volume", Long.class),
                rs.getBigDecimal("trading_amount"), rs.getBigDecimal("previous_close")),
                publicBenchmarkCode, asOf).stream().findFirst();
    }

    @Override
    public Optional<java.math.BigDecimal> finalWeatherScore(String publicBenchmarkCode, LocalDate tradeDate) {
        if (publicBenchmarkCode == null || publicBenchmarkCode.isBlank() || tradeDate == null) {
            throw new IllegalArgumentException("Benchmark code and trade date are required");
        }
        return Optional.ofNullable(jdbc.query("""
                SELECT weather.score
                FROM index_weather weather
                JOIN index_series series ON series.id = weather.series_id
                JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
                JOIN market_index market ON market.id = series.index_id
                JOIN index_daily target ON target.series_id = weather.series_id
                    AND target.trade_date = weather.trade_date
                WHERE market.public_benchmark_code = ? AND series.is_active AND series.is_canonical
                  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
                  AND weather.trade_date = ? AND weather.status = 'FINAL'
                  AND target.price_status = 'FINAL'
                  AND weather.input_manifest ->> 'source' = 'index_daily'
                  AND weather.input_manifest ->> 'seriesId' = series.id::text
                  AND weather.input_manifest ->> 'targetDate' = weather.trade_date::text
                  AND jsonb_typeof(weather.input_manifest -> 'observations') = 'array'
                  AND EXISTS (
                      SELECT 1 FROM jsonb_array_elements(weather.input_manifest -> 'observations') AS input(value)
                      WHERE input.value ->> 'date' = weather.trade_date::text
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM jsonb_array_elements(weather.input_manifest -> 'observations') AS input(value)
                      LEFT JOIN index_daily source_day
                        ON source_day.series_id = weather.series_id
                       AND source_day.trade_date::text = input.value ->> 'date'
                      WHERE source_day.series_id IS NULL
                         OR source_day.price_status <> 'FINAL'
                         OR source_day.revision::text IS DISTINCT FROM input.value ->> 'revision'
                  )
                """, rs -> rs.next() ? rs.getBigDecimal("score") : null, publicBenchmarkCode, tradeDate));
    }
}
