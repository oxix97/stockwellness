-- Apply only after eod-read-seed.sql in a disposable database dedicated to #339.
-- Benchmark dates must match the independent BacktestEngineTest input: the engine
-- uses the union of all available benchmark and asset dates for its simulations.
DELETE FROM index_daily WHERE series_id IN (9101, 9102, 9105);
INSERT INTO index_daily (series_id, trade_date, open_value, high_value, low_value, close_value, volume, trading_amount, price_status, price_source, collected_at, revision)
VALUES
 (9101, '2025-01-02', 100, 100, 100, 100, 1, 100, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1),
 (9101, '2025-01-15', 105, 105, 105, 105, 1, 105, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1),
 (9101, '2026-01-02', 110, 110, 110, 110, 1, 110, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1);
INSERT INTO stock (id, market_code, ticker, standard_code, name, market_type, currency, sector_index_id, sector_code, sector_name, status, is_premium_tracking, is_preferred)
VALUES (9907, 'KOSPI', '990107', 'KR7990107007', '합성백테스트', 'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', false, false);
INSERT INTO stock_price_daily (stock_id, trade_date, quote_scope, open_price, high_price, low_price, close_price, volume, trading_amount, data_status, source, collected_at, revision)
VALUES
 (9907, '2025-01-02', 'KRX', 100, 100, 100, 100, 1, 100, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1),
 (9907, '2025-01-15', 'KRX', 110, 110, 110, 110, 1, 110, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1),
 (9907, '2026-01-02', 'KRX', 110, 110, 110, 110, 1, 110, 'FINAL', 'FIXTURE:T339', '2026-01-02 18:00:00+09', 1);
INSERT INTO portfolio (id, member_id, name, description, configuration_version, created_at)
VALUES (9003, 9001, 'Backtest synthetic portfolio', 'Task 339 independent oracle', 1, '2025-01-02 09:00:00+09');
INSERT INTO portfolio_item (id, portfolio_id, stock_id, asset_type, purchase_date, quantity, average_purchase_price, currency, target_weight, created_at)
VALUES (9003, 9003, 9907, 'STOCK', '2025-01-02', 10, 100, 'KRW', 100, '2025-01-02 09:00:00+09');
