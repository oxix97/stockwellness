package org.stockwellness.adapter.out.persistence.stock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.price.PriceBasis;

@Component
@RequiredArgsConstructor
public class StockPriceReadAdapter implements StockPriceReadPort {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<Long, List<PriceObservation>> history(Set<Long> stockIds, String quoteScope,
            LocalDate start, LocalDate end, PriceBasis basis) {
        if (stockIds == null || stockIds.isEmpty()) return Map.of();
        if (start == null || end == null || start.isAfter(end)) throw new IllegalArgumentException("Invalid price date range");
        return query(stockIds, quoteScope, "p.trade_date BETWEEN :start AND :end", basis,
                new MapSqlParameterSource()
                        .addValue("stockIds", stockIds)
                        .addValue("quoteScope", quoteScope)
                        .addValue("start", start)
                        .addValue("end", end),
                "p.trade_date ASC");
    }

    @Override
    public Map<Long, List<PriceObservation>> recent(Set<Long> stockIds, String quoteScope,
            LocalDate asOf, int limit, PriceBasis basis) {
        if (stockIds == null || stockIds.isEmpty()) return Map.of();
        if (asOf == null || limit < 1) throw new IllegalArgumentException("Invalid recent price query");
        Map<Long, List<PriceObservation>> results = new LinkedHashMap<>();
        stockIds.stream().sorted().forEach(id -> results.put(id, new ArrayList<>()));
        String sql = """
                WITH ranked AS (
                    SELECT p.stock_id, p.trade_date, p.quote_scope,
                           %s,
                           p.volume, p.trading_amount, p.data_status, p.revision,
                           ROW_NUMBER() OVER (PARTITION BY p.stock_id ORDER BY p.trade_date DESC) AS row_number
                    FROM stock_price_daily p
                    %s
                    WHERE p.stock_id IN (:stockIds) AND p.quote_scope = :quoteScope
                      AND p.trade_date <= :asOf AND p.data_status = 'FINAL'
                )
                SELECT stock_id, trade_date, quote_scope, open_price, high_price, low_price, close_price,
                       volume, trading_amount, data_status, revision
                FROM ranked WHERE row_number <= :limit
                ORDER BY stock_id, trade_date DESC
                """.formatted(priceColumns(basis), joinFor(basis));
        jdbc.query(sql, new MapSqlParameterSource()
                .addValue("stockIds", stockIds)
                .addValue("quoteScope", quoteScope)
                .addValue("asOf", asOf)
                .addValue("limit", limit), rowMapper(basis))
                .forEach(value -> results.get(value.stockId()).add(value));
        return results;
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

    private Map<Long, List<PriceObservation>> query(Set<Long> stockIds, String quoteScope, String where,
            PriceBasis basis, MapSqlParameterSource parameters, String orderBy) {
        Map<Long, List<PriceObservation>> results = new LinkedHashMap<>();
        stockIds.stream().sorted().forEach(id -> results.put(id, new ArrayList<>()));
        String sql = """
                SELECT p.stock_id, p.trade_date, p.quote_scope,
                       %s,
                       p.volume, p.trading_amount, p.data_status, p.revision
                FROM stock_price_daily p
                %s
                WHERE p.stock_id IN (:stockIds) AND p.quote_scope = :quoteScope
                  AND p.data_status = 'FINAL' AND %s
                ORDER BY p.stock_id, %s
                """.formatted(priceColumns(basis), joinFor(basis), where, orderBy);
        jdbc.query(sql, parameters, rowMapper(basis)).forEach(value -> results.get(value.stockId()).add(value));
        return results;
    }

    private static String joinFor(PriceBasis basis) {
        return switch (basis) {
            case RAW -> "";
            case PROVIDER_ADJUSTED -> """
                    JOIN stock_price_adjustment a
                      ON a.stock_id = p.stock_id AND a.trade_date = p.trade_date AND a.quote_scope = p.quote_scope
                    """;
        };
    }

    private static String priceColumns(PriceBasis basis) {
        return switch (basis) {
            case RAW -> "p.open_price, p.high_price, p.low_price, p.close_price";
            case PROVIDER_ADJUSTED -> "a.open_price, a.high_price, a.low_price, a.adjusted_close AS close_price";
        };
    }

    private static RowMapper<PriceObservation> rowMapper(PriceBasis basis) {
        return (rs, rowNum) -> map(rs, basis);
    }

    private static PriceObservation map(ResultSet rs, PriceBasis basis) throws SQLException {
        return new PriceObservation(rs.getLong("stock_id"), rs.getObject("trade_date", LocalDate.class),
                rs.getString("quote_scope"), basis, rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"),
                rs.getBigDecimal("low_price"), rs.getBigDecimal("close_price"), rs.getObject("volume", Long.class),
                rs.getBigDecimal("trading_amount"), rs.getString("data_status"), rs.getInt("revision"));
    }
}
