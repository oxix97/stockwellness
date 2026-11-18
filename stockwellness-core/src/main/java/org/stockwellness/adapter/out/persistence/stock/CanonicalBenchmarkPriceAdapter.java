package org.stockwellness.adapter.out.persistence.stock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

@Component
@RequiredArgsConstructor
public class CanonicalBenchmarkPriceAdapter implements BenchmarkPricePort {
    private static final String SELECT = """
            SELECT market_index.name, market_index.public_benchmark_code AS ticker, daily.trade_date AS base_date,
                   daily.open_value, daily.high_value, daily.low_value, daily.close_value,
                   daily.volume,
                   (daily.close_value - previous.close_value) / NULLIF(previous.close_value, 0) * 100 AS change_rate
            FROM index_daily daily
            JOIN index_series series ON series.id = daily.series_id
            JOIN market_index ON market_index.id = series.index_id
            LEFT JOIN LATERAL (
                SELECT close_value FROM index_daily
                WHERE series_id = daily.series_id AND trade_date < daily.trade_date AND close_value IS NOT NULL
                ORDER BY trade_date DESC LIMIT 1
            ) previous ON true
            WHERE market_index.public_benchmark_code = ?
              AND series.is_active AND series.is_canonical
              AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
            """;

    private static final RowMapper<BenchmarkPrice> PRICE_MAPPER = CanonicalBenchmarkPriceAdapter::map;
    private final JdbcTemplate jdbc;
    private final IndexSeriesRepository seriesRepository;
    private final IndexDailyPort indexDailyPort;

    @Override
    public Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate) {
        return jdbc.query(SELECT + " AND daily.trade_date = ?", PRICE_MAPPER, ticker, baseDate).stream().findFirst();
    }

    @Override
    public Optional<BenchmarkPrice> findLatestBefore(String ticker, LocalDate baseDate) {
        return jdbc.query(SELECT + " AND daily.trade_date < ? AND daily.close_value IS NOT NULL ORDER BY daily.trade_date DESC LIMIT 1",
                PRICE_MAPPER, ticker, baseDate).stream().findFirst();
    }

    @Override
    public List<BenchmarkPrice> findHistoryByTicker(String ticker, LocalDate endDate, int limit) {
        if (limit < 1) throw new IllegalArgumentException("History limit must be positive");
        return jdbc.query(SELECT + " AND daily.trade_date <= ? ORDER BY daily.trade_date DESC LIMIT ?",
                PRICE_MAPPER, ticker, endDate, limit);
    }

    @Override
    public void save(BenchmarkPrice price) {
        BenchmarkType type = BenchmarkType.fromTicker(price.getTicker());
        if (type == BenchmarkType.NASDAQ_COMPOSITE || type == BenchmarkType.PHLX_SEMICONDUCTOR) {
            throw new IllegalArgumentException("Unsupported canonical benchmark: " + price.getTicker());
        }
        String currency = type.isOverseas() ? "USD" : "KRW";
        List<IndexSeries> series = seriesRepository.findCanonical(price.getTicker(), "PRICE", "NONE", currency);
        if (series.size() != 1) throw new IllegalStateException("Canonical benchmark series is not configured: " + price.getTicker());
        IndexDaily daily = IndexDaily.of(series.getFirst(), price.getBaseDate(), price.getOpenPrice(),
                price.getHighPrice(), price.getLowPrice(), price.getClosePrice(), price.getVolume(),
                null, null, null, null, null, null, null, null,
                "KIS:benchmark-sync", null, BenchmarkDataStatus.PROVISIONAL, null);
        indexDailyPort.upsert(List.of(daily));
    }

    private static BenchmarkPrice map(ResultSet row, int rowNumber) throws SQLException {
        BenchmarkPrice price = BenchmarkPrice.of(row.getString("name"), row.getString("ticker"),
                row.getObject("base_date", LocalDate.class), row.getBigDecimal("close_value"));
        price.updatePrices(row.getBigDecimal("open_value"), row.getBigDecimal("high_value"),
                row.getBigDecimal("low_value"), row.getBigDecimal("close_value"),
                row.getBigDecimal("change_rate"), row.getObject("volume", Long.class));
        return price;
    }
}
