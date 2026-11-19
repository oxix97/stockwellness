-- Synthetic, reproducible EOD verification fixture for Task #337
-- Target schema: 2026-11-11 redesigned PostgreSQL schema (37 tables + 04-reference-data.sql)
-- Fixed reference date (asOfDate): 2026-11-18
-- Previous trading date: 2026-11-17
-- Earlier trading date: 2026-11-16
-- Provisional trap date: 2026-11-19 (must be excluded by FINAL-only read paths)
-- Future FINAL trap date: 2099-12-31 (must be excluded by trade_date <= asOfDate filters)

BEGIN;

-- 1. Trading calendar (KRX/KOSPI sample window)
INSERT INTO trading_calendar (market_code, trade_date, is_trading_day, opens_at, closes_at, source, collected_at)
VALUES
    ('KOSPI', DATE '2026-11-16', true, TIMESTAMPTZ '2026-11-16 09:00:00+09', TIMESTAMPTZ '2026-11-16 15:30:00+09', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    ('KOSPI', DATE '2026-11-17', true, TIMESTAMPTZ '2026-11-17 09:00:00+09', TIMESTAMPTZ '2026-11-17 15:30:00+09', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    ('KOSPI', DATE '2026-11-18', true, TIMESTAMPTZ '2026-11-18 09:00:00+09', TIMESTAMPTZ '2026-11-18 15:30:00+09', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    ('KOSPI', DATE '2026-11-19', true, TIMESTAMPTZ '2026-11-19 09:00:00+09', TIMESTAMPTZ '2026-11-19 15:30:00+09', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 09:05:00+09');

-- 2. Market benchmarks + synthetic sector index
INSERT INTO market_index (id, index_key, name, kind, public_benchmark_code, created_at)
VALUES
    (9101, 'FIXTURE:KOSPI',       '코스피 종합',    'MARKET', '0001', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9102, 'FIXTURE:KOSDAQ',      '코스닥 종합',    'MARKET', '1001', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9103, 'FIXTURE:KOSPI200',    '코스피 200',     'MARKET', '2001', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9104, 'FIXTURE:KOSDAQ150',   '코스닥 150',     'MARKET', '3003', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9105, 'FIXTURE:SPX',         'S&P 500',        'MARKET', 'SPX',  TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9106, 'FIXTURE:NDX',         '나스닥 100',     'MARKET', 'NDX',  TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9107, 'FIXTURE:DJI',         '다우존스 산업',  'MARKET', '.DJI', TIMESTAMPTZ '2026-11-18 18:00:00+09'),
    (9108, 'FIXTURE:SECTOR-0013', '합성전기전자',   'SECTOR', NULL,   TIMESTAMPTZ '2026-11-18 18:00:00+09');

INSERT INTO index_series (
    id, index_id, provider, provider_namespace, provider_series_code,
    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical
)
VALUES
    (9101, 9101, 'SYNTH', 'fixture.t337', '0001', 'KOSPI',  'PRICE', 'NONE', 'KRW', true, true),
    (9102, 9102, 'SYNTH', 'fixture.t337', '1001', 'KOSDAQ', 'PRICE', 'NONE', 'KRW', true, true),
    (9103, 9103, 'SYNTH', 'fixture.t337', '2001', 'KOSPI',  'PRICE', 'NONE', 'KRW', true, true),
    (9104, 9104, 'SYNTH', 'fixture.t337', '3003', 'KOSDAQ', 'PRICE', 'NONE', 'KRW', true, true),
    (9105, 9105, 'SYNTH', 'fixture.t337', 'SPX',  'NYSE',   'PRICE', 'NONE', 'USD', true, true),
    (9106, 9106, 'SYNTH', 'fixture.t337', 'NDX',  'NASDAQ', 'PRICE', 'NONE', 'USD', true, true),
    (9107, 9107, 'SYNTH', 'fixture.t337', '.DJI', 'NYSE',   'PRICE', 'NONE', 'USD', true, true);

-- 3. Index daily observations (3 FINAL days + PROVISIONAL + future FINAL traps on 0001)
INSERT INTO index_daily (
    series_id, trade_date, open_value, high_value, low_value, close_value,
    volume, trading_amount, price_status, price_source, collected_at, revision
)
VALUES
    -- 0001 (KOSPI): +5.00% on 2026-11-18
    (9101, DATE '2026-11-16', 2470.0000, 2490.0000, 2465.0000, 2480.0000, 410000000, 8100000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9101, DATE '2026-11-17', 2485.0000, 2510.0000, 2480.0000, 2500.0000, 420000000, 8500000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9101, DATE '2026-11-18', 2510.0000, 2630.0000, 2505.0000, 2625.0000, 500000000, 9900000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9101, DATE '2026-11-19', 2650.0000, 2720.0000, 2640.0000, 2700.0000, 120000000, 2100000000000.00, 'PROVISIONAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 10:00:00+09', 1),
    (9101, DATE '2099-12-31', 9900.0000, 9999.0000, 9890.0000, 9999.0000, 1000, 1000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 1001 (KOSDAQ): -1.00% on 2026-11-18
    (9102, DATE '2026-11-16', 802.0000, 808.0000, 800.0000, 805.0000, 900000000, 6000000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9102, DATE '2026-11-17', 804.0000, 806.0000, 798.0000, 800.0000, 920000000, 6100000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9102, DATE '2026-11-18', 799.0000, 801.0000, 790.0000, 792.0000, 950000000, 6200000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 2001 (KOSPI 200): +2.50% on 2026-11-18
    (9103, DATE '2026-11-16', 329.0000, 331.0000, 328.0000, 330.0000, 150000000, 4500000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9103, DATE '2026-11-17', 330.5000, 333.0000, 330.0000, 332.0000, 155000000, 4600000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9103, DATE '2026-11-18', 333.0000, 341.0000, 332.5000, 340.3000, 170000000, 5100000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 3003 (KOSDAQ 150): -1.00% on 2026-11-18
    (9104, DATE '2026-11-16', 1308.0000, 1315.0000, 1305.0000, 1310.0000, 80000000, 1800000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9104, DATE '2026-11-17', 1305.0000, 1309.0000, 1298.0000, 1300.0000, 82000000, 1850000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9104, DATE '2026-11-18', 1298.0000, 1301.0000, 1285.0000, 1287.0000, 85000000, 1900000000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- SPX (S&P 500): +1.00% on 2026-11-18
    (9105, DATE '2026-11-16', 5790.0000, 5810.0000, 5785.0000, 5800.0000, 2100000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 07:00:00+09', 1),
    (9105, DATE '2026-11-17', 5810.0000, 5860.0000, 5805.0000, 5850.0000, 2200000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 07:00:00+09', 1),
    (9105, DATE '2026-11-18', 5860.0000, 5915.0000, 5855.0000, 5908.5000, 2300000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 07:00:00+09', 1),

    -- NDX (NASDAQ 100): +1.50% on 2026-11-18
    (9106, DATE '2026-11-16', 20150.0000, 20250.0000, 20100.0000, 20200.0000, 1100000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 07:00:00+09', 1),
    (9106, DATE '2026-11-17', 20100.0000, 20180.0000, 19950.0000, 20000.0000, 1150000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 07:00:00+09', 1),
    (9106, DATE '2026-11-18', 20050.0000, 20340.0000, 20020.0000, 20300.0000, 1200000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 07:00:00+09', 1),

    -- .DJI (Dow Jones): -0.50% on 2026-11-18
    (9107, DATE '2026-11-16', 43050.0000, 43150.0000, 43000.0000, 43100.0000, 350000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 07:00:00+09', 1),
    (9107, DATE '2026-11-17', 43080.0000, 43120.0000, 42950.0000, 43000.0000, 360000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 07:00:00+09', 1),
    (9107, DATE '2026-11-18', 42980.0000, 43010.0000, 42750.0000, 42785.0000, 370000000, 0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 07:00:00+09', 1);

-- 4. Market weather (FINAL on 2026-11-18 = score 80 -> SUNNY / STEADY_ADVANCE; PROVISIONAL trap on 2026-11-19)
INSERT INTO index_weather (series_id, trade_date, score, status, score_version, input_manifest, calculated_at)
VALUES
    (
        9101,
        DATE '2026-11-18',
        80.00,
        'FINAL',
        'eod-fixture-v1',
        '{"source":"index_daily","seriesId":"9101","targetDate":"2026-11-18","observations":[{"date":"2026-11-18","revision":1}]}'::jsonb,
        TIMESTAMPTZ '2026-11-18 18:10:00+09'
    ),
    (
        9101,
        DATE '2026-11-19',
        15.00,
        'PROVISIONAL',
        'eod-fixture-v1',
        '{"source":"index_daily","seriesId":"9101","targetDate":"2026-11-19","observations":[{"date":"2026-11-19","revision":1}]}'::jsonb,
        TIMESTAMPTZ '2026-11-19 10:05:00+09'
    );

-- 5. Synthetic KRX stocks (990101..990106)
INSERT INTO stock (
    id, market_code, ticker, standard_code, name, market_type, currency,
    sector_index_id, sector_code, sector_name, status, is_premium_tracking, is_preferred
)
VALUES
    (9901, 'KOSPI', '990101', 'KR7990101001', '합성알파전자',   'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', true,  false),
    (9902, 'KOSPI', '990102', 'KR7990102002', '합성베타바이오', 'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', true,  false),
    (9903, 'KOSPI', '990103', 'KR7990103003', '합성감마소재',   'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', false, false),
    (9904, 'KOSPI', '990104', 'KR7990104004', '합성델타물류',   'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', false, false),
    (9905, 'KOSPI', '990105', 'KR7990105005', '합성엡실론누락', 'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', false, false),
    (9906, 'KOSPI', '990106', 'KR7990106006', '합성제타중공업', 'KOSPI', 'KRW', 9108, '0013', '합성전기전자', 'ACTIVE', false, false);

-- 6. Daily stock prices (KRX)
INSERT INTO stock_price_daily (
    stock_id, trade_date, quote_scope, open_price, high_price, low_price, close_price,
    volume, trading_amount, data_status, source, collected_at, revision
)
VALUES
    -- 990101: 9600 -> 10000 -> 11000 (+10.00%), plus PROVISIONAL (2026-11-19) & future FINAL (2099-12-31) traps
    (9901, DATE '2026-11-16', 'KRX',  9500.0000,  9700.0000,  9400.0000,  9600.0000, 100000,  960000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9901, DATE '2026-11-17', 'KRX',  9800.0000, 10100.0000,  9700.0000, 10000.0000, 120000, 1200000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9901, DATE '2026-11-18', 'KRX', 10200.0000, 11200.0000, 10100.0000, 11000.0000, 150000, 1650000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9901, DATE '2026-11-19', 'KRX', 11500.0000, 12000.0000, 11400.0000, 11900.0000,  50000,  595000000.00, 'PROVISIONAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 10:00:00+09', 1),
    (9901, DATE '2099-12-31', 'KRX', 99000.0000, 99500.0000, 98500.0000, 99000.0000,   1000,   99000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 990102: 21500 -> 20000 -> 19000 (-5.00%)
    (9902, DATE '2026-11-16', 'KRX', 21400.0000, 21800.0000, 21200.0000, 21500.0000,  70000, 1505000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-16 18:00:00+09', 1),
    (9902, DATE '2026-11-17', 'KRX', 21000.0000, 21200.0000, 19900.0000, 20000.0000,  75000, 1500000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9902, DATE '2026-11-18', 'KRX', 19800.0000, 20100.0000, 18900.0000, 19000.0000,  80000, 1520000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 990103: 50000 -> 52000 (+4.00%)
    (9903, DATE '2026-11-17', 'KRX', 49500.0000, 50500.0000, 49200.0000, 50000.0000,  35000, 1750000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9903, DATE '2026-11-18', 'KRX', 50500.0000, 52500.0000, 50000.0000, 52000.0000,  40000, 2080000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 990104: 30000 -> 27000 (-10.00%)
    (9904, DATE '2026-11-17', 'KRX', 30500.0000, 30800.0000, 29800.0000, 30000.0000,  55000, 1650000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9904, DATE '2026-11-18', 'KRX', 29500.0000, 29600.0000, 26900.0000, 27000.0000,  60000, 1620000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- 990105: PROVISIONAL only (no FINAL price -> ranking currentPrice/fluctuationRate/transactionAmount must be NULL)
    (9905, DATE '2026-11-18', 'KRX', 14900.0000, 15200.0000, 14800.0000, 15000.0000,  10000,  150000000.00, 'PROVISIONAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 15:35:00+09', 1),

    -- 990106: 40000 -> 40400 (+1.00%)
    (9906, DATE '2026-11-17', 'KRX', 39800.0000, 40300.0000, 39700.0000, 40000.0000,  25000, 1000000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-17 18:00:00+09', 1),
    (9906, DATE '2026-11-18', 'KRX', 40200.0000, 41000.0000, 39800.0000, 40400.0000,  30000, 1212000000.00, 'FINAL',       'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1);

-- 7. Adjusted prices on 990101 (RAW vs PROVIDER_ADJUSTED distinction pre-2026-11-18)
INSERT INTO stock_price_adjustment (
    stock_id, trade_date, quote_scope, adjustment_basis, adjustment_as_of,
    open_price, high_price, low_price, adjusted_close, source, collected_at
)
VALUES
    (9901, DATE '2026-11-16', 'KRX', 'PROVIDER_ADJUSTED', DATE '2026-11-18',  4750.0000,  4850.0000,  4700.0000,  4800.0000, 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:05:00+09'),
    (9901, DATE '2026-11-17', 'KRX', 'PROVIDER_ADJUSTED', DATE '2026-11-18',  4900.0000,  5050.0000,  4850.0000,  5000.0000, 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:05:00+09'),
    (9901, DATE '2026-11-18', 'KRX', 'PROVIDER_ADJUSTED', DATE '2026-11-18', 10200.0000, 11200.0000, 10100.0000, 11000.0000, 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:05:00+09');

-- 8. Technical indicators on 990101 (RAW, valid input_manifest matching revision=1 FINAL rows)
INSERT INTO stock_technical_indicator (
    stock_id, trade_date, quote_scope, price_basis,
    ma5, ma20, ma20_disparity, ma60, ma120,
    input_manifest, algorithm_version, calculated_at
)
VALUES
    (
        9901, DATE '2026-11-16', 'KRX', 'RAW',
        9500.0000, 9300.0000, 103.225806, 9100.0000, 9000.0000,
        '{"source":"stock_price_daily","sourceId":"9901","quoteScope":"KRX","priceBasis":"RAW","targetDate":"2026-11-16","observations":[{"date":"2026-11-16","revision":1}]}'::jsonb,
        'eod-fixture-v1', TIMESTAMPTZ '2026-11-16 18:10:00+09'
    ),
    (
        9901, DATE '2026-11-17', 'KRX', 'RAW',
        9750.0000, 9500.0000, 105.263158, 9250.0000, 9080.0000,
        '{"source":"stock_price_daily","sourceId":"9901","quoteScope":"KRX","priceBasis":"RAW","targetDate":"2026-11-17","observations":[{"date":"2026-11-17","revision":1}]}'::jsonb,
        'eod-fixture-v1', TIMESTAMPTZ '2026-11-17 18:10:00+09'
    ),
    (
        9901, DATE '2026-11-18', 'KRX', 'RAW',
        10200.0000, 9800.0000, 112.244898, 9500.0000, 9200.0000,
        '{"source":"stock_price_daily","sourceId":"9901","quoteScope":"KRX","priceBasis":"RAW","targetDate":"2026-11-18","observations":[{"date":"2026-11-18","revision":1}]}'::jsonb,
        'eod-fixture-v1', TIMESTAMPTZ '2026-11-18 18:10:00+09'
    );

-- 9. Investor supply daily (KRX, ties + negatives + zeros + PROVISIONAL trap on 2026-11-19)
INSERT INTO stock_investor_trade_daily (
    stock_id, trade_date, quote_scope, investor_type,
    net_quantity, net_amount, data_status, source, collected_at, revision
)
VALUES
    -- BUY side (2026-11-18 FINAL)
    (9901, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL',  50000.000000,  550000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9901, DATE '2026-11-18', 'KRX', 'FOREIGN',            30000.000000,  330000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    (9902, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL',  20000.000000,  380000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9902, DATE '2026-11-18', 'KRX', 'FOREIGN',            17368.000000,  330000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    (9903, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL',   7308.000000,  380000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9903, DATE '2026-11-18', 'KRX', 'FOREIGN',             4000.000000,  208000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- SELL side (2026-11-18 FINAL) + zero foreign net_amount on 990106
    (9904, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL', -18519.000000, -500000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9904, DATE '2026-11-18', 'KRX', 'FOREIGN',           -22222.000000, -600000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    (9905, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL', -20000.000000, -300000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9905, DATE '2026-11-18', 'KRX', 'FOREIGN',           -10000.000000, -150000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    (9906, DATE '2026-11-18', 'KRX', 'INSTITUTION_TOTAL',  -7426.000000, -300000000.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),
    (9906, DATE '2026-11-18', 'KRX', 'FOREIGN',                0.000000,          0.00, 'FINAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-18 18:00:00+09', 1),

    -- PROVISIONAL trap on 2026-11-19 (must NOT move effectiveDate to 2026-11-19)
    (9901, DATE '2026-11-19', 'KRX', 'INSTITUTION_TOTAL',  99999.000000,  999999999.00, 'PROVISIONAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 10:00:00+09', 1),
    (9901, DATE '2026-11-19', 'KRX', 'FOREIGN',            99999.000000,  999999999.00, 'PROVISIONAL', 'FIXTURE:SYNTHETIC', TIMESTAMPTZ '2026-11-19 10:00:00+09', 1);

-- 10. Dedicated synthetic member & portfolio (Portfolio ID = 9001)
INSERT INTO member (
    id, nickname, role, risk_level, status,
    notification_rebalancing, notification_market_alert, notification_new_listing, created_at
)
VALUES (
    9001, 'eod-fixture-user', 'USER', 'MEDIUM', 'ACTIVE',
    true, false, false, TIMESTAMPTZ '2026-11-16 09:00:00+09'
);

INSERT INTO member_identity (id, member_id, provider, subject, created_at)
VALUES (9001, 9001, 'KAKAO', 'eod-fixture@synthetic.test', TIMESTAMPTZ '2026-11-16 09:00:00+09');

INSERT INTO portfolio (id, member_id, name, description, configuration_version, created_at)
VALUES (
    9001, 9001, 'EOD Synthetic Read Portfolio',
    'Task 337 fixed EOD read verification portfolio', 1, TIMESTAMPTZ '2026-11-16 09:00:00+09'
);

INSERT INTO portfolio_item (
    id, portfolio_id, stock_id, asset_type, purchase_date,
    quantity, average_purchase_price, currency, target_weight, created_at
)
VALUES
    (9001, 9001, 9901, 'STOCK', DATE '2026-11-16', 100.000000,  9500.0000, 'KRW', 60.000000, TIMESTAMPTZ '2026-11-16 09:00:00+09'),
    (9002, 9001, 9902, 'STOCK', DATE '2026-11-16',  50.000000, 20000.0000, 'KRW', 40.000000, TIMESTAMPTZ '2026-11-16 09:00:00+09');

-- 11. Align identity sequences after explicit ID inserts
SELECT setval(pg_get_serial_sequence('market_index', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM market_index), 1), true);
SELECT setval(pg_get_serial_sequence('index_series', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM index_series), 1), true);
SELECT setval(pg_get_serial_sequence('stock', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM stock), 1), true);
SELECT setval(pg_get_serial_sequence('member', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM member), 1), true);
SELECT setval(pg_get_serial_sequence('member_identity', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM member_identity), 1), true);
SELECT setval(pg_get_serial_sequence('portfolio', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM portfolio), 1), true);
SELECT setval(pg_get_serial_sequence('portfolio_item', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM portfolio_item), 1), true);

COMMIT;
