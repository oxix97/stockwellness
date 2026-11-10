-- Market identity and exchange calendar are additive. Existing Stock IDs remain stable.
CREATE TABLE market (
    code varchar(20) PRIMARY KEY,
    name varchar(100) NOT NULL,
    country_code char(2) NOT NULL,
    currency char(3) NOT NULL,
    timezone varchar(50) NOT NULL
);

INSERT INTO market (code, name, country_code, currency, timezone) VALUES
    ('KOSPI', '코스피', 'KR', 'KRW', 'Asia/Seoul'),
    ('KOSDAQ', '코스닥', 'KR', 'KRW', 'Asia/Seoul'),
    ('NASDAQ', 'NASDAQ', 'US', 'USD', 'America/New_York'),
    ('NYSE', 'NYSE', 'US', 'USD', 'America/New_York'),
    ('AMEX', 'AMEX', 'US', 'USD', 'America/New_York');

ALTER TABLE stock ADD COLUMN market_code varchar(20);
ALTER TABLE stock ADD COLUMN listed_shares bigint;
ALTER TABLE stock ADD COLUMN listed_shares_as_of date;
ALTER TABLE stock ADD COLUMN source_updated_at timestamptz;

-- INDEX rows are synthetic legacy index instruments, not listed stocks. Keep them
-- nullable until benchmark consumers have moved to BenchmarkSeries.
UPDATE stock SET market_code = market_type WHERE market_type <> 'INDEX';

ALTER TABLE stock ADD CONSTRAINT fk_stock_market FOREIGN KEY (market_code) REFERENCES market(code);
ALTER TABLE stock ADD CONSTRAINT ck_stock_listed_shares_as_of
    CHECK ((listed_shares IS NULL) = (listed_shares_as_of IS NULL));
ALTER TABLE stock ADD CONSTRAINT ck_stock_listed_shares_positive
    CHECK (listed_shares IS NULL OR listed_shares > 0);
ALTER TABLE stock DROP CONSTRAINT IF EXISTS stock_ticker_key;
ALTER TABLE stock DROP CONSTRAINT IF EXISTS idx_stock_ticker;
ALTER TABLE stock ADD CONSTRAINT uq_stock_market_ticker UNIQUE (market_code, ticker);
CREATE INDEX IF NOT EXISTS idx_stock_ticker ON stock (ticker);

CREATE TABLE trading_calendar (
    market_code varchar(20) NOT NULL REFERENCES market(code),
    trade_date date NOT NULL,
    is_trading_day boolean NOT NULL,
    opens_at timestamptz,
    closes_at timestamptz,
    source varchar(50) NOT NULL,
    collected_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (market_code, trade_date),
    CONSTRAINT ck_calendar_session CHECK (
        (opens_at IS NULL AND closes_at IS NULL)
        OR (opens_at IS NOT NULL AND closes_at IS NOT NULL AND opens_at < closes_at)
    ),
    CONSTRAINT ck_calendar_closed CHECK (is_trading_day OR (opens_at IS NULL AND closes_at IS NULL))
);

-- No risk rows are backfilled: existing flags lack a verified complete source snapshot.
CREATE TABLE stock_risk_status (
    stock_id bigint PRIMARY KEY REFERENCES stock(id),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','HALTED','ADMINISTRATIVE','DELISTED')),
    is_trading_halt boolean NOT NULL,
    is_clearing_trade boolean NOT NULL,
    is_administered boolean NOT NULL,
    market_warning_level varchar(2),
    is_warning_notice boolean NOT NULL,
    is_unfaithful_disclosure boolean NOT NULL,
    is_backdoor_listing boolean NOT NULL,
    is_short_term_overheat boolean NOT NULL,
    is_short_sell_overheat boolean NOT NULL,
    is_abnormal_surge boolean NOT NULL,
    is_invest_caution boolean NOT NULL,
    source_api varchar(50) NOT NULL,
    source_updated_at timestamptz,
    collected_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
