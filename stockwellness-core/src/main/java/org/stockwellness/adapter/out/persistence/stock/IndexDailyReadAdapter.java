package org.stockwellness.adapter.out.persistence.stock;

import java.time.LocalDate;
import java.util.List;
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
    public List<IndexPriceObservation> recentFinal(long seriesId, LocalDate asOf, int limit) {
        if (seriesId <= 0 || asOf == null || limit < 1) {
            throw new IllegalArgumentException("Index series, date, and positive limit are required");
        }
        return jdbc.query("""
                SELECT series_id, trade_date, high_value, low_value, close_value, advance_count, decline_count,
                       unchanged_count, upper_limit_count, lower_limit_count, revision, price_status
                FROM index_daily
                WHERE series_id = ? AND trade_date <= ? AND price_status = 'FINAL'
                  AND high_value IS NOT NULL AND low_value IS NOT NULL AND close_value IS NOT NULL
                ORDER BY trade_date DESC
                LIMIT ?
                """, (rs, rowNum) -> new IndexPriceObservation(rs.getLong("series_id"),
                rs.getObject("trade_date", LocalDate.class), rs.getBigDecimal("high_value"),
                rs.getBigDecimal("low_value"), rs.getBigDecimal("close_value"),
                rs.getObject("advance_count", Integer.class), rs.getObject("decline_count", Integer.class),
                rs.getObject("unchanged_count", Integer.class), rs.getObject("upper_limit_count", Integer.class),
                rs.getObject("lower_limit_count", Integer.class), rs.getInt("revision"),
                rs.getString("price_status")), seriesId, asOf, limit);
    }
}
