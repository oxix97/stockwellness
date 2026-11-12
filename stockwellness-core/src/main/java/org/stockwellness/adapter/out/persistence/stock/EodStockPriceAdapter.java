package org.stockwellness.adapter.out.persistence.stock;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;

@Component
@RequiredArgsConstructor
public class EodStockPriceAdapter implements EodStockPricePort {
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

    @Override
    @Transactional
    public void upsert(List<EodStockPrice> values) {
        if (values == null || values.isEmpty()) return;
        jdbc.batchUpdate(UPSERT_PRICE, values, values.size(), (ps, value) -> setPrice(ps, value));
    }

    @Override
    @Transactional
    public void upsertAdjustments(List<StockPriceAdjustment> values) {
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
