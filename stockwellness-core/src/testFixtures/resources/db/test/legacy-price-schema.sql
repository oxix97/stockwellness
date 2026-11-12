CREATE TABLE stock_price_daily (
    stock_id bigint NOT NULL REFERENCES stock(id),
    trade_date date NOT NULL,
    quote_scope varchar(20) NOT NULL,
    open_price numeric(19,4),
    high_price numeric(19,4),
    low_price numeric(19,4),
    close_price numeric(19,4),
    volume bigint CHECK (volume >= 0),
    trading_amount numeric(25,2),
    data_status varchar(20) NOT NULL CHECK (data_status IN ('FINAL','PROVISIONAL','INCOMPLETE')),
    source varchar(100) NOT NULL,
    collected_at timestamptz NOT NULL,
    revision integer NOT NULL DEFAULT 1 CHECK (revision >= 1),
    corrected_at timestamptz,
    PRIMARY KEY (stock_id, trade_date, quote_scope),
    CHECK (high_price IS NULL OR low_price IS NULL OR high_price >= low_price),
    CHECK (high_price IS NULL OR open_price IS NULL OR high_price >= open_price),
    CHECK (high_price IS NULL OR close_price IS NULL OR high_price >= close_price),
    CHECK (low_price IS NULL OR open_price IS NULL OR low_price <= open_price),
    CHECK (low_price IS NULL OR close_price IS NULL OR low_price <= close_price),
    CHECK (data_status <> 'FINAL' OR
        (open_price IS NOT NULL AND high_price IS NOT NULL AND low_price IS NOT NULL AND close_price IS NOT NULL))
);
CREATE INDEX idx_stock_price_daily_history ON stock_price_daily(stock_id, quote_scope, trade_date DESC);

CREATE TABLE stock_price_adjustment (
    stock_id bigint NOT NULL,
    trade_date date NOT NULL,
    quote_scope varchar(20) NOT NULL,
    adjustment_basis varchar(30) NOT NULL CHECK (adjustment_basis = 'PROVIDER_ADJUSTED'),
    adjustment_as_of date NOT NULL,
    open_price numeric(19,4),
    high_price numeric(19,4),
    low_price numeric(19,4),
    adjusted_close numeric(19,4) NOT NULL,
    source varchar(100) NOT NULL,
    collected_at timestamptz NOT NULL,
    PRIMARY KEY (stock_id, trade_date, quote_scope),
    FOREIGN KEY (stock_id, trade_date, quote_scope)
        REFERENCES stock_price_daily(stock_id, trade_date, quote_scope),
    CHECK (high_price IS NULL OR low_price IS NULL OR high_price >= low_price),
    CHECK (adjustment_as_of >= trade_date)
);
