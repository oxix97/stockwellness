package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;

@Component
@RequiredArgsConstructor
public class EodStockPriceAdapter implements EodStockPricePort {
    private static final int WRITE_CHUNK_SIZE = 500;

    private static final String UPSERT_PRICE = """
            INSERT INTO stock_price_daily AS existing
                (stock_id, trade_date, quote_scope, open_price, high_price, low_price, close_price,
                 volume, trading_amount, data_status, source, collected_at, revision)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT (stock_id, trade_date, quote_scope) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                volume = EXCLUDED.volume,
                trading_amount = EXCLUDED.trading_amount,
                data_status = EXCLUDED.data_status,
                source = EXCLUDED.source,
                collected_at = EXCLUDED.collected_at,
                revision = existing.revision + CASE WHEN
                    (existing.open_price, existing.high_price, existing.low_price, existing.close_price,
                     existing.volume, existing.trading_amount, existing.data_status, existing.source)
                    IS DISTINCT FROM
                    (EXCLUDED.open_price, EXCLUDED.high_price, EXCLUDED.low_price, EXCLUDED.close_price,
                     EXCLUDED.volume, EXCLUDED.trading_amount, EXCLUDED.data_status, EXCLUDED.source)
                    THEN 1 ELSE 0 END,
                corrected_at = CASE WHEN
                    (existing.open_price, existing.high_price, existing.low_price, existing.close_price,
                     existing.volume, existing.trading_amount, existing.data_status, existing.source)
                    IS DISTINCT FROM
                    (EXCLUDED.open_price, EXCLUDED.high_price, EXCLUDED.low_price, EXCLUDED.close_price,
                     EXCLUDED.volume, EXCLUDED.trading_amount, EXCLUDED.data_status, EXCLUDED.source)
                    THEN EXCLUDED.collected_at ELSE existing.corrected_at END
            WHERE CASE existing.data_status
                WHEN 'FINAL' THEN EXCLUDED.data_status = 'FINAL'
                WHEN 'PROVISIONAL' THEN EXCLUDED.data_status IN ('PROVISIONAL', 'FINAL')
                ELSE TRUE
            END
            """;

    private static final String UPSERT_ADJUSTMENT = """
            INSERT INTO stock_price_adjustment
                (stock_id, trade_date, quote_scope, adjustment_basis, adjustment_as_of,
                 open_price, high_price, low_price, adjusted_close, source, collected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (stock_id, trade_date, quote_scope) DO UPDATE SET
                adjustment_basis = EXCLUDED.adjustment_basis,
                adjustment_as_of = EXCLUDED.adjustment_as_of,
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                adjusted_close = EXCLUDED.adjusted_close,
                source = EXCLUDED.source,
                collected_at = EXCLUDED.collected_at
            WHERE EXCLUDED.adjustment_as_of >= stock_price_adjustment.adjustment_as_of
            """;

    private final JdbcTemplate jdbc;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void upsert(List<EodStockPrice> values) {
        writePrices(values);
    }

    @Override
    @Transactional
    public void upsertObservations(List<EodStockPrice> prices, List<StockPriceAdjustment> adjustments) {
        writePrices(prices);
        writeAdjustments(adjustments);
    }

    private void writePrices(List<EodStockPrice> values) {
        if (values == null || values.isEmpty()) return;
        ensureUniquePriceKeys(values);
        for (int start = 0; start < values.size(); start += WRITE_CHUNK_SIZE) {
            List<EodStockPrice> chunk = values.subList(start, Math.min(start + WRITE_CHUNK_SIZE, values.size()));
            Map<PriceKey, PriceState> previous = lockAndLoadPriceStates(chunk);
            jdbc.batchUpdate(UPSERT_PRICE, chunk, chunk.size(), (ps, value) -> setPrice(ps, value));
            for (EodStockPrice value : chunk) {
                PriceKey key = keyOf(value);
                PriceState before = previous.get(key);
                if (before != null && "FINAL".equals(before.status())
                        && "FINAL".equals(value.getDataStatus()) && before.differsFrom(value)) {
                    saveRevisionEvent(MarketDataRevisionEvent.stock(key.stockId(), key.tradeDate(), key.quoteScope(),
                            before.revision() + 1));
                }
            }
        }
    }

    private Map<PriceKey, PriceState> lockAndLoadPriceStates(List<EodStockPrice> values) {
        String lockValues = placeholders(values.size(), "(?::text)");
        String lockSql = """
                SELECT pg_advisory_xact_lock(hashtextextended(keys.lock_key, 0))
                FROM (VALUES %s) AS keys(lock_key)
                ORDER BY keys.lock_key
                """.formatted(lockValues);
        jdbc.query(lockSql, ps -> {
            for (int index = 0; index < values.size(); index++) {
                ps.setString(index + 1, lockKey(keyOf(values.get(index))));
            }
        }, rs -> {
            while (rs.next()) {
                // Consume all rows so PostgreSQL acquires every transaction-scoped key lock.
            }
            return null;
        });

        String keyValues = placeholders(values.size(), "(?::bigint, ?::date, ?::text)");
        String selectSql = """
                SELECT price.stock_id, price.trade_date, price.quote_scope, price.open_price, price.high_price,
                       price.low_price, price.close_price, price.volume, price.trading_amount,
                       price.data_status, price.source, price.revision
                FROM (VALUES %s) AS input_keys(stock_id, trade_date, quote_scope)
                JOIN stock_price_daily price
                  ON price.stock_id = input_keys.stock_id
                 AND price.trade_date = input_keys.trade_date
                 AND price.quote_scope = input_keys.quote_scope
                ORDER BY price.stock_id, price.trade_date, price.quote_scope
                FOR UPDATE OF price
                """.formatted(keyValues);
        Map<PriceKey, PriceState> states = new HashMap<>();
        jdbc.query(selectSql, ps -> {
            int parameter = 1;
            for (EodStockPrice value : values) {
                PriceKey key = keyOf(value);
                ps.setLong(parameter++, key.stockId());
                ps.setDate(parameter++, java.sql.Date.valueOf(key.tradeDate()));
                ps.setString(parameter++, key.quoteScope());
            }
        }, rs -> {
            while (rs.next()) {
                PriceKey key = new PriceKey(rs.getLong("stock_id"), rs.getObject("trade_date", LocalDate.class),
                        rs.getString("quote_scope"));
                long volume = rs.getLong("volume");
                Long nullableVolume = rs.wasNull() ? null : volume;
                states.put(key, new PriceState(rs.getInt("revision"), rs.getString("data_status"),
                        rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"),
                        rs.getBigDecimal("low_price"), rs.getBigDecimal("close_price"), nullableVolume,
                        rs.getBigDecimal("trading_amount"), rs.getString("source")));
            }
            return null;
        });
        return states;
    }

    private static void ensureUniquePriceKeys(List<EodStockPrice> values) {
        Set<PriceKey> keys = new HashSet<>();
        for (EodStockPrice value : values) {
            if (value == null || !keys.add(keyOf(value))) {
                throw new IllegalArgumentException("Stock price batch contains a duplicate natural key");
            }
        }
    }

    private static PriceKey keyOf(EodStockPrice value) {
        return new PriceKey(value.getId().getStockId(), value.getId().getTradeDate(), value.getId().getQuoteScope());
    }

    private static String lockKey(PriceKey key) {
        return key.stockId() + ":" + key.tradeDate() + ":" + key.quoteScope();
    }

    private static String placeholders(int count, String tuple) {
        return String.join(",", Collections.nCopies(count, tuple));
    }

    @Override
    @Transactional
    public void upsertAdjustments(List<StockPriceAdjustment> values) {
        writeAdjustments(values);
    }

    private void writeAdjustments(List<StockPriceAdjustment> values) {
        if (values == null || values.isEmpty()) return;
        jdbc.batchUpdate(UPSERT_ADJUSTMENT, values, values.size(), (ps, value) -> setAdjustment(ps, value));
    }

    private static void setPrice(PreparedStatement ps, EodStockPrice value) throws SQLException {
        int i = 1;
        ps.setLong(i++, value.getId().getStockId());
        ps.setDate(i++, java.sql.Date.valueOf(value.getId().getTradeDate()));
        ps.setString(i++, value.getId().getQuoteScope());
        ps.setBigDecimal(i++, value.getOpenPrice());
        ps.setBigDecimal(i++, value.getHighPrice());
        ps.setBigDecimal(i++, value.getLowPrice());
        ps.setBigDecimal(i++, value.getClosePrice());
        setNullableLong(ps, i++, value.getVolume());
        ps.setBigDecimal(i++, value.getTradingAmount());
        ps.setString(i++, value.getDataStatus());
        ps.setString(i++, value.getSource());
        ps.setTimestamp(i, Timestamp.from(value.getCollectedAt()));
    }

    private void saveRevisionEvent(MarketDataRevisionEvent event) {
        try {
            outboxPort.save(OutboxEvent.create(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize market data revision event", ex);
        }
    }

    private record PriceKey(long stockId, LocalDate tradeDate, String quoteScope) {}

    private record PriceState(int revision, String status, BigDecimal openPrice,
            BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
            Long volume, BigDecimal tradingAmount, String source) {
        private boolean differsFrom(EodStockPrice value) {
            return !same(openPrice, value.getOpenPrice()) || !same(highPrice, value.getHighPrice())
                    || !same(lowPrice, value.getLowPrice()) || !same(closePrice, value.getClosePrice())
                    || !Objects.equals(volume, value.getVolume())
                    || !same(tradingAmount, value.getTradingAmount())
                    || !Objects.equals(status, value.getDataStatus())
                    || !Objects.equals(source, value.getSource());
        }

        private static boolean same(BigDecimal left, BigDecimal right) {
            return left == null ? right == null : right != null && left.compareTo(right) == 0;
        }
    }

    private static void setAdjustment(PreparedStatement ps, StockPriceAdjustment value) throws SQLException {
        int i = 1;
        ps.setLong(i++, value.getId().getStockId());
        ps.setDate(i++, java.sql.Date.valueOf(value.getId().getTradeDate()));
        ps.setString(i++, value.getId().getQuoteScope());
        ps.setString(i++, value.getAdjustmentBasis());
        ps.setDate(i++, java.sql.Date.valueOf(value.getAdjustmentAsOf()));
        ps.setBigDecimal(i++, value.getOpenPrice());
        ps.setBigDecimal(i++, value.getHighPrice());
        ps.setBigDecimal(i++, value.getLowPrice());
        ps.setBigDecimal(i++, value.getAdjustedClose());
        ps.setString(i++, value.getSource());
        ps.setTimestamp(i, Timestamp.from(value.getCollectedAt()));
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, value);
    }
}
