-- Adapter-aligned SQL cross-checks and EXPLAIN (ANALYZE, BUFFERS) for Task #337
-- Run against the isolated PostgreSQL instance after loading eod-read-seed.sql:
--   psql -X -v ON_ERROR_STOP=1 "$ISOLATED_DB_URL" -f k6/fixtures/eod-read-sql-check.sql

-- ============================================================================
-- 0. Seed Integrity Summary (counts, date range, EOD statuses)
-- ============================================================================
SELECT 'market_index' AS table_name, COUNT(*) AS row_count FROM market_index
UNION ALL
SELECT 'index_series', COUNT(*) FROM index_series
UNION ALL
SELECT 'index_daily', COUNT(*) FROM index_daily
UNION ALL
SELECT 'index_weather', COUNT(*) FROM index_weather
UNION ALL
SELECT 'stock', COUNT(*) FROM stock
UNION ALL
SELECT 'stock_price_daily', COUNT(*) FROM stock_price_daily
UNION ALL
SELECT 'stock_price_adjustment', COUNT(*) FROM stock_price_adjustment
UNION ALL
SELECT 'stock_technical_indicator', COUNT(*) FROM stock_technical_indicator
UNION ALL
SELECT 'stock_investor_trade_daily', COUNT(*) FROM stock_investor_trade_daily
UNION ALL
SELECT 'member', COUNT(*) FROM member
UNION ALL
SELECT 'portfolio', COUNT(*) FROM portfolio
UNION ALL
SELECT 'portfolio_item', COUNT(*) FROM portfolio_item;

-- ============================================================================
-- 1. IndexDailyReadAdapter.historyFinal + finalWeatherScore
--    Matches MarketIndexService.getMarketIndexes()
-- ============================================================================
WITH final_history AS (
    SELECT market_index.public_benchmark_code AS ticker,
           series.id AS series_id,
           daily.trade_date,
           daily.close_value,
           LAG(daily.close_value) OVER (PARTITION BY series.id ORDER BY daily.trade_date) AS previous_close
    FROM index_daily daily
    JOIN index_series series ON series.id = daily.series_id
    JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
    JOIN market_index market_index ON market_index.id = series.index_id
    WHERE market_index.public_benchmark_code IN ('0001', '1001', '2001', '3003', 'SPX', 'NDX', '.DJI')
      AND series.is_active AND series.is_canonical
      AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
      AND daily.trade_date <= DATE '2026-11-19'
      AND daily.price_status = 'FINAL'
)
SELECT ticker,
       trade_date,
       close_value,
       previous_close,
       ROUND(((close_value - previous_close) / previous_close * 100)::numeric, 2) AS fluctuation_rate
FROM final_history
WHERE trade_date = DATE '2026-11-18'
ORDER BY ticker;

SELECT weather.trade_date, weather.score, weather.status
FROM index_weather weather
JOIN index_series series ON series.id = weather.series_id
JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
JOIN market_index market ON market.id = series.index_id
JOIN index_daily target ON target.series_id = weather.series_id
    AND target.trade_date = weather.trade_date
WHERE market.public_benchmark_code = '0001'
  AND series.is_active AND series.is_canonical
  AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
  AND weather.trade_date = DATE '2026-11-18'
  AND weather.status = 'FINAL'
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
  );

-- ============================================================================
-- 2. StockInvestorTradeReadAdapter.latestFinalDate + topStocks (BUY & SELL)
-- ============================================================================
SELECT MAX(trade_date) AS effective_date
FROM (
    SELECT trade_date, stock_id
    FROM stock_investor_trade_daily
    WHERE quote_scope = 'KRX' AND data_status = 'FINAL'
      AND investor_type IN ('FOREIGN', 'INSTITUTION_TOTAL')
    GROUP BY trade_date, stock_id
    HAVING COUNT(DISTINCT investor_type) = 2
) complete_dates;

-- INSTITUTION_TOTAL BUY
WITH final_prices AS (
    SELECT stock_id, trade_date, close_price, trading_amount,
           LAG(close_price) OVER (PARTITION BY stock_id, quote_scope ORDER BY trade_date) AS previous_close
    FROM stock_price_daily
    WHERE quote_scope = 'KRX' AND data_status = 'FINAL' AND trade_date <= DATE '2026-11-18'
), latest_prices AS (
    SELECT DISTINCT ON (stock_id) stock_id, trade_date, close_price, trading_amount, previous_close
    FROM final_prices
    ORDER BY stock_id, trade_date DESC
)
SELECT stock.ticker, stock.name AS stock_name, sector.name AS sector_name,
       price.close_price AS current_price,
       CASE WHEN price.close_price IS NULL OR price.previous_close IS NULL OR price.previous_close <= 0 THEN NULL
            ELSE (price.close_price - price.previous_close) / price.previous_close * 100
       END AS fluctuation_rate,
       flow.net_quantity, flow.net_amount, price.trading_amount AS transaction_amount
FROM stock_investor_trade_daily flow
JOIN stock ON stock.id = flow.stock_id
LEFT JOIN market_index sector ON sector.id = stock.sector_index_id
LEFT JOIN latest_prices price ON price.stock_id = flow.stock_id
WHERE flow.trade_date = DATE '2026-11-18'
  AND flow.quote_scope = 'KRX'
  AND flow.investor_type = 'INSTITUTION_TOTAL'
  AND flow.data_status = 'FINAL'
  AND flow.net_amount > 0
ORDER BY flow.net_amount DESC, stock.ticker ASC
LIMIT 10;

-- FOREIGN SELL
WITH final_prices AS (
    SELECT stock_id, trade_date, close_price, trading_amount,
           LAG(close_price) OVER (PARTITION BY stock_id, quote_scope ORDER BY trade_date) AS previous_close
    FROM stock_price_daily
    WHERE quote_scope = 'KRX' AND data_status = 'FINAL' AND trade_date <= DATE '2026-11-18'
), latest_prices AS (
    SELECT DISTINCT ON (stock_id) stock_id, trade_date, close_price, trading_amount, previous_close
    FROM final_prices
    ORDER BY stock_id, trade_date DESC
)
SELECT stock.ticker, stock.name AS stock_name, sector.name AS sector_name,
       price.close_price AS current_price,
       CASE WHEN price.close_price IS NULL OR price.previous_close IS NULL OR price.previous_close <= 0 THEN NULL
            ELSE (price.close_price - price.previous_close) / price.previous_close * 100
       END AS fluctuation_rate,
       flow.net_quantity, flow.net_amount, price.trading_amount AS transaction_amount
FROM stock_investor_trade_daily flow
JOIN stock ON stock.id = flow.stock_id
LEFT JOIN market_index sector ON sector.id = stock.sector_index_id
LEFT JOIN latest_prices price ON price.stock_id = flow.stock_id
WHERE flow.trade_date = DATE '2026-11-18'
  AND flow.quote_scope = 'KRX'
  AND flow.investor_type = 'FOREIGN'
  AND flow.data_status = 'FINAL'
  AND flow.net_amount < 0
ORDER BY flow.net_amount ASC, stock.ticker ASC
LIMIT 10;

-- ============================================================================
-- 3. StockPriceReadAdapter.history (RAW + PROVIDER_ADJUSTED) & Indicators for 990101
-- ============================================================================
SELECT p.trade_date,
       p.open_price,
       p.high_price,
       p.low_price,
       p.close_price AS raw_close,
       a.adjusted_close AS adj_close,
       p.volume,
       p.trading_amount,
       i.ma5,
       i.ma20,
       i.ma60,
       i.ma120
FROM stock_price_daily p
JOIN stock s ON s.id = p.stock_id
LEFT JOIN stock_price_adjustment a
  ON a.stock_id = p.stock_id AND a.trade_date = p.trade_date AND a.quote_scope = p.quote_scope
LEFT JOIN stock_technical_indicator i
  ON i.stock_id = p.stock_id AND i.trade_date = p.trade_date AND i.quote_scope = p.quote_scope
 AND i.price_basis = 'RAW'
 AND i.input_manifest ->> 'source' = 'stock_price_daily'
 AND i.input_manifest ->> 'sourceId' = p.stock_id::text
 AND i.input_manifest ->> 'quoteScope' = p.quote_scope
 AND i.input_manifest ->> 'priceBasis' = i.price_basis
 AND i.input_manifest ->> 'targetDate' = i.trade_date::text
WHERE s.ticker = '990101'
  AND p.quote_scope = 'KRX'
  AND p.data_status = 'FINAL'
  AND p.trade_date BETWEEN DATE '2025-11-19' AND DATE '2026-11-19'
ORDER BY p.trade_date ASC;

-- ============================================================================
-- 4. Portfolio Valuation Cross-Check (Portfolio 9001, RAW recent 2 FINAL rows)
-- ============================================================================
WITH ranked_prices AS (
    SELECT p.stock_id,
           p.trade_date,
           p.close_price,
           LAG(p.close_price) OVER (PARTITION BY p.stock_id, p.quote_scope ORDER BY p.trade_date) AS previous_close,
           ROW_NUMBER() OVER (PARTITION BY p.stock_id ORDER BY p.trade_date DESC) AS rn
    FROM stock_price_daily p
    WHERE p.quote_scope = 'KRX'
      AND p.data_status = 'FINAL'
      AND p.trade_date <= DATE '2026-11-19'
)
SELECT MAX(rp.trade_date) AS as_of_date,
       ROUND(SUM(pi.quantity * pi.average_purchase_price), 0) AS total_purchase_amount,
       ROUND(SUM(pi.quantity * rp.close_price), 0) AS current_total_value,
       ROUND(SUM(pi.quantity * rp.close_price) - SUM(pi.quantity * pi.average_purchase_price), 0) AS total_profit_loss,
       ROUND((SUM(pi.quantity * rp.close_price) - SUM(pi.quantity * pi.average_purchase_price))
             / SUM(pi.quantity * pi.average_purchase_price) * 100, 4) AS total_return_rate,
       ROUND(SUM(pi.quantity * rp.close_price) - SUM(pi.quantity * rp.previous_close), 0) AS daily_profit_loss,
       ROUND((SUM(pi.quantity * rp.close_price) - SUM(pi.quantity * rp.previous_close))
             / SUM(pi.quantity * rp.previous_close) * 100, 4) AS daily_return_rate
FROM portfolio_item pi
JOIN ranked_prices rp ON rp.stock_id = pi.stock_id AND rp.rn = 1
WHERE pi.portfolio_id = 9001;

-- ============================================================================
-- 5. EXPLAIN (ANALYZE, BUFFERS) for Core Read Adapter Queries
-- ============================================================================
EXPLAIN (ANALYZE, BUFFERS)
WITH final_history AS (
    SELECT series.id AS series_id, daily.trade_date, daily.close_value,
           LAG(daily.close_value) OVER (PARTITION BY series.id ORDER BY daily.trade_date) AS previous_close
    FROM index_daily daily
    JOIN index_series series ON series.id = daily.series_id
    JOIN market calendar ON calendar.code = series.calendar_market_code AND calendar.currency = series.currency
    JOIN market_index market_index ON market_index.id = series.index_id
    WHERE market_index.public_benchmark_code = '0001'
      AND series.is_active AND series.is_canonical
      AND series.return_basis = 'PRICE' AND series.dividend_basis = 'NONE'
      AND daily.trade_date <= DATE '2026-11-19' AND daily.price_status = 'FINAL'
)
SELECT * FROM final_history WHERE trade_date >= DATE '2026-10-20' ORDER BY trade_date ASC;

EXPLAIN (ANALYZE, BUFFERS)
WITH final_prices AS (
    SELECT stock_id, trade_date, close_price, trading_amount,
           LAG(close_price) OVER (PARTITION BY stock_id, quote_scope ORDER BY trade_date) AS previous_close
    FROM stock_price_daily
    WHERE quote_scope = 'KRX' AND data_status = 'FINAL' AND trade_date <= DATE '2026-11-18'
), latest_prices AS (
    SELECT DISTINCT ON (stock_id) stock_id, trade_date, close_price, trading_amount, previous_close
    FROM final_prices
    ORDER BY stock_id, trade_date DESC
)
SELECT stock.ticker, stock.name AS stock_name, sector.name AS sector_name,
       price.close_price AS current_price,
       CASE WHEN price.close_price IS NULL OR price.previous_close IS NULL OR price.previous_close <= 0 THEN NULL
            ELSE (price.close_price - price.previous_close) / price.previous_close * 100
       END AS fluctuation_rate,
       flow.net_quantity, flow.net_amount, price.trading_amount AS transaction_amount
FROM stock_investor_trade_daily flow
JOIN stock ON stock.id = flow.stock_id
LEFT JOIN market_index sector ON sector.id = stock.sector_index_id
LEFT JOIN latest_prices price ON price.stock_id = flow.stock_id
WHERE flow.trade_date = DATE '2026-11-18' AND flow.quote_scope = 'KRX'
  AND flow.investor_type = 'INSTITUTION_TOTAL' AND flow.data_status = 'FINAL'
  AND flow.net_amount > 0
ORDER BY flow.net_amount DESC, stock.ticker ASC
LIMIT 10;

EXPLAIN (ANALYZE, BUFFERS)
WITH final_prices AS (
    SELECT p.stock_id, p.trade_date, p.quote_scope,
           p.open_price AS open_price, p.high_price AS high_price,
           p.low_price AS low_price, p.close_price AS close_price,
           a.adjusted_close, p.volume, p.trading_amount, p.data_status, p.revision,
           LAG(p.close_price) OVER (PARTITION BY p.stock_id, p.quote_scope ORDER BY p.trade_date) AS previous_close
    FROM stock_price_daily p
    LEFT JOIN stock_price_adjustment a
      ON a.stock_id = p.stock_id AND a.trade_date = p.trade_date AND a.quote_scope = p.quote_scope
    WHERE p.stock_id IN (9901) AND p.quote_scope = 'KRX'
      AND p.trade_date <= DATE '2026-11-19' AND p.data_status = 'FINAL'
), ranked AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY stock_id ORDER BY trade_date DESC) AS row_number
    FROM final_prices
)
SELECT p.stock_id, p.trade_date, p.quote_scope, p.open_price, p.high_price, p.low_price, p.close_price,
       p.adjusted_close, p.volume, p.trading_amount, p.data_status, p.revision, p.previous_close,
       i.ma5, i.ma20, i.ma60, i.ma120
FROM ranked p
LEFT JOIN stock_technical_indicator i
  ON i.stock_id = p.stock_id AND i.trade_date = p.trade_date AND i.quote_scope = p.quote_scope
 AND i.price_basis = 'RAW'
 AND i.input_manifest ->> 'source' = 'stock_price_daily'
 AND i.input_manifest ->> 'sourceId' = p.stock_id::text
 AND i.input_manifest ->> 'quoteScope' = p.quote_scope
 AND i.input_manifest ->> 'priceBasis' = i.price_basis
 AND i.input_manifest ->> 'targetDate' = i.trade_date::text
WHERE p.trade_date BETWEEN DATE '2025-11-19' AND DATE '2026-11-19'
ORDER BY p.stock_id, p.trade_date ASC;
