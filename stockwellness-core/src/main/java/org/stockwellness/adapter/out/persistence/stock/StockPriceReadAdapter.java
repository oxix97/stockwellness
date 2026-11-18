package org.stockwellness.adapter.out.persistence.stock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.InvalidPriceObservation;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.price.PriceBasis;

@Component
@RequiredArgsConstructor
public class StockPriceReadAdapter implements StockPriceReadPort {
    private static final String FINAL_PRICES = """
            SELECT p.stock_id, p.trade_date, p.quote_scope, %s,
                   a.adjusted_close, p.volume, p.trading_amount, p.data_status, p.revision,
                   LAG(%s) OVER (PARTITION BY p.stock_id, p.quote_scope ORDER BY p.trade_date) AS previous_close
            FROM stock_price_daily p
            LEFT JOIN stock_price_adjustment a
              ON a.stock_id = p.stock_id AND a.trade_date = p.trade_date AND a.quote_scope = p.quote_scope
            WHERE p.stock_id IN (:stockIds) AND p.quote_scope = :quoteScope
              AND p.trade_date <= :asOf AND p.data_status = 'FINAL'
              AND (:basis = 'RAW' OR a.adjusted_close IS NOT NULL)
            """;

    private static final String VALID_INDICATOR_JOIN = """
            LEFT JOIN stock_technical_indicator i
              ON i.stock_id = p.stock_id AND i.trade_date = p.trade_date AND i.quote_scope = p.quote_scope
             AND i.price_basis = :basis
             AND i.input_manifest ->> 'source' = 'stock_price_daily'
             AND i.input_manifest ->> 'sourceId' = p.stock_id::text
             AND i.input_manifest ->> 'quoteScope' = p.quote_scope
             AND i.input_manifest ->> 'priceBasis' = i.price_basis
             AND i.input_manifest ->> 'targetDate' = i.trade_date::text
             AND jsonb_typeof(i.input_manifest -> 'observations') = 'array'
             AND EXISTS (
                   SELECT 1 FROM jsonb_array_elements(i.input_manifest -> 'observations') AS input(value)
                   WHERE input.value ->> 'date' = i.trade_date::text
             )
             AND NOT EXISTS (
                   SELECT 1 FROM jsonb_array_elements(i.input_manifest -> 'observations') AS input(value)
                   LEFT JOIN stock_price_daily source_price
                     ON source_price.stock_id = i.stock_id
                    AND source_price.quote_scope = i.quote_scope
                    AND source_price.trade_date::text = input.value ->> 'date'
                   WHERE source_price.stock_id IS NULL
                      OR source_price.data_status <> 'FINAL'
                      OR source_price.revision::text IS DISTINCT FROM input.value ->> 'revision'
             )
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<InvalidPriceObservation> invalidPriceObservations(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("Invalid price integrity date range");
        }
        return jdbc.getJdbcTemplate().query("""
                SELECT DISTINCT ON (p.trade_date, s.ticker, s.id)
                       s.ticker, s.name, p.trade_date, p.close_price
                FROM stock_price_daily p
                JOIN stock s ON s.id = p.stock_id
                WHERE p.trade_date BETWEEN ? AND ?
                  AND (p.close_price IS NULL OR p.close_price <= 0)
                ORDER BY p.trade_date, s.ticker, s.id, (p.close_price IS NULL) DESC, p.close_price ASC, p.quote_scope ASC
                """, (rs, rowNum) -> new InvalidPriceObservation(
                rs.getString("ticker"), rs.getString("name"), rs.getObject("trade_date", LocalDate.class),
                rs.getBigDecimal("close_price")), start, end);
    }

    @Override
    public Map<Long, List<PriceObservation>> history(Set<Long> stockIds, String quoteScope,
            LocalDate start, LocalDate end, PriceBasis basis) {
        if (stockIds == null || stockIds.isEmpty()) return Map.of();
        if (quoteScope == null || quoteScope.isBlank() || basis == null
                || start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("Invalid price history query");
        }
        return query(stockIds, quoteScope, basis, "p.trade_date BETWEEN :start AND :end", "p.trade_date ASC",
                new MapSqlParameterSource().addValue("stockIds", stockIds).addValue("quoteScope", quoteScope)
                        .addValue("start", start).addValue("end", end).addValue("asOf", end)
                        .addValue("basis", basis.name()));
    }

    @Override
    public Map<Long, List<PriceObservation>> recent(Set<Long> stockIds, String quoteScope,
            LocalDate asOf, int limit, PriceBasis basis) {
        if (stockIds == null || stockIds.isEmpty()) return Map.of();
        if (quoteScope == null || quoteScope.isBlank() || basis == null || asOf == null || limit < 1) {
            throw new IllegalArgumentException("Invalid recent price query");
        }
        return query(stockIds, quoteScope, basis, "p.row_number <= :limit", "p.trade_date DESC",
                new MapSqlParameterSource().addValue("stockIds", stockIds).addValue("quoteScope", quoteScope)
                        .addValue("asOf", asOf).addValue("limit", limit).addValue("basis", basis.name()));
    }

    @Override
    public Map<Long, PriceObservation> latest(Set<Long> stockIds, String quoteScope,
            LocalDate asOf, PriceBasis basis) {
        Map<Long, List<PriceObservation>> recent = recent(stockIds, quoteScope, asOf, 1, basis);
        Map<Long, PriceObservation> latest = new LinkedHashMap<>();
        recent.forEach((stockId, values) -> {
            if (!values.isEmpty()) latest.put(stockId, values.getFirst());
        });
        return latest;
    }

    @Override
    public Optional<LocalDate> latestFinalDate(String quoteScope, LocalDate asOf) {
        if (quoteScope == null || quoteScope.isBlank() || asOf == null) {
            throw new IllegalArgumentException("Quote scope and as-of date are required");
        }
        return Optional.ofNullable(jdbc.getJdbcTemplate().queryForObject("""
                SELECT MAX(trade_date) FROM stock_price_daily
                WHERE quote_scope = ? AND trade_date <= ? AND data_status = 'FINAL'
                """, LocalDate.class, quoteScope, asOf));
    }

    @Override
    public List<LocalDate> finalDatesFrom(long stockId, String quoteScope, LocalDate startDate, int limit) {
        if (stockId <= 0 || quoteScope == null || quoteScope.isBlank() || startDate == null || limit < 1) {
            throw new IllegalArgumentException("Stock, quote scope, start date, and positive limit are required");
        }
        return jdbc.getJdbcTemplate().query("""
                SELECT trade_date FROM stock_price_daily
                WHERE stock_id = ? AND quote_scope = ? AND trade_date >= ? AND data_status = 'FINAL'
                ORDER BY trade_date LIMIT ?
                """, (rs, rowNum) -> rs.getObject("trade_date", LocalDate.class), stockId, quoteScope, startDate, limit);
    }

    private Map<Long, List<PriceObservation>> query(Set<Long> stockIds, String quoteScope, PriceBasis basis,
            String filter, String order, MapSqlParameterSource parameters) {
        Map<Long, List<PriceObservation>> results = new LinkedHashMap<>();
        stockIds.stream().sorted().forEach(id -> results.put(id, new ArrayList<>()));
        String selectedClose = basis == PriceBasis.RAW ? "p.close_price" : "a.adjusted_close";
        String sourcePrices = basis == PriceBasis.RAW
                ? "p.open_price AS open_price, p.high_price AS high_price, p.low_price AS low_price, p.close_price AS close_price"
                : "a.open_price AS open_price, a.high_price AS high_price, a.low_price AS low_price, a.adjusted_close AS close_price";
        String selectedPriceColumns = "p.open_price, p.high_price, p.low_price, p.close_price";
        String sql = """
                WITH final_prices AS (
                    %s
                ), ranked AS (
                    SELECT *, ROW_NUMBER() OVER (PARTITION BY stock_id ORDER BY trade_date DESC) AS row_number
                    FROM final_prices
                )
                SELECT p.stock_id, p.trade_date, p.quote_scope, %s, p.adjusted_close, p.volume, p.trading_amount,
                       p.data_status, p.revision, p.previous_close,
                       i.ma5, i.ma20, i.ma60, i.ma120, i.rsi14, i.macd, i.macd_signal, i.adx, i.plus_di, i.minus_di
                FROM ranked p
                %s
                WHERE %s
                ORDER BY p.stock_id, %s
                """.formatted(FINAL_PRICES.formatted(sourcePrices, selectedClose), selectedPriceColumns,
                        VALID_INDICATOR_JOIN, filter, order);
        jdbc.query(sql, parameters, rowMapper(basis)).forEach(value -> results.get(value.stockId()).add(value));
        return results;
    }

    private static RowMapper<PriceObservation> rowMapper(PriceBasis basis) {
        return (rs, rowNum) -> map(rs, basis);
    }

    private static PriceObservation map(ResultSet rs, PriceBasis basis) throws SQLException {
        return new PriceObservation(rs.getLong("stock_id"), rs.getObject("trade_date", LocalDate.class),
                rs.getString("quote_scope"), basis, rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"),
                rs.getBigDecimal("low_price"), rs.getBigDecimal("close_price"), rs.getObject("volume", Long.class),
                rs.getBigDecimal("trading_amount"), rs.getString("data_status"), rs.getInt("revision"),
                rs.getBigDecimal("adjusted_close"), rs.getBigDecimal("previous_close"), rs.getBigDecimal("ma5"),
                rs.getBigDecimal("ma20"), rs.getBigDecimal("ma60"), rs.getBigDecimal("ma120"),
                rs.getBigDecimal("rsi14"), rs.getBigDecimal("macd"), rs.getBigDecimal("macd_signal"),
                rs.getBigDecimal("adx"), rs.getBigDecimal("plus_di"), rs.getBigDecimal("minus_di"));
    }
}
