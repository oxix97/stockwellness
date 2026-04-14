package org.stockwellness.batch.job.stockprice.sync.support;

public final class StockPriceSql {

    public static final String UPSERT_STOCK_PRICE = """
            INSERT INTO stock_price (
                base_date, stock_id, open_price, high_price, low_price, close_price, adj_close_price, prev_close_price, volume, transaction_amt,
                ma5, ma20, ma60, ma120, rsi14, macd, macd_signal,
                bollinger_upper, bollinger_mid, bollinger_lower, adx, plus_di, minus_di,
                alignment_status, is_golden_cross, is_dead_cross, is_macd_cross,
                created_at
            ) VALUES (
                CAST(? AS date), CAST(? AS bigint), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS bigint), CAST(? AS numeric),
                CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric),
                CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric),
                CAST(? AS varchar), CAST(? AS boolean), CAST(? AS boolean), CAST(? AS boolean),
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (base_date, stock_id) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                adj_close_price = EXCLUDED.adj_close_price,
                prev_close_price = EXCLUDED.prev_close_price,
                volume = EXCLUDED.volume,
                transaction_amt = EXCLUDED.transaction_amt,
                ma5 = EXCLUDED.ma5,
                ma20 = EXCLUDED.ma20,
                ma60 = EXCLUDED.ma60,
                ma120 = EXCLUDED.ma120,
                rsi14 = EXCLUDED.rsi14,
                macd = EXCLUDED.macd,
                macd_signal = EXCLUDED.macd_signal,
                bollinger_upper = EXCLUDED.bollinger_upper,
                bollinger_mid = EXCLUDED.bollinger_mid,
                bollinger_lower = EXCLUDED.bollinger_lower,
                adx = EXCLUDED.adx,
                plus_di = EXCLUDED.plus_di,
                minus_di = EXCLUDED.minus_di,
                alignment_status = EXCLUDED.alignment_status,
                is_golden_cross = EXCLUDED.is_golden_cross,
                is_dead_cross = EXCLUDED.is_dead_cross,
                is_macd_cross = EXCLUDED.is_macd_cross
            """;

    private StockPriceSql() {
    }
}
