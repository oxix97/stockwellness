ALTER TABLE sector_insight
    ADD COLUMN advance_ratio numeric(10,2);

-- These entities inherit updated_at from AbstractEntity, but the original
-- weather migration did not create the column.
ALTER TABLE market_weather ADD COLUMN updated_at timestamp(6);
ALTER TABLE sector_weather ADD COLUMN updated_at timestamp(6);
ALTER TABLE sector_indicator ADD COLUMN updated_at timestamp(6);
ALTER TABLE sector_weather ALTER COLUMN ai_title TYPE varchar(200);

CREATE TABLE sector_leading_stock (
    index_id bigint NOT NULL,
    trade_date date NOT NULL,
    rank integer NOT NULL CHECK (rank BETWEEN 1 AND 5),
    stock_id bigint NOT NULL,
    price_date date NOT NULL,
    quote_scope varchar(10) NOT NULL,
    PRIMARY KEY (index_id, trade_date, rank),
    UNIQUE (index_id, trade_date, stock_id),
    FOREIGN KEY (index_id, trade_date)
        REFERENCES index_daily(index_id, trade_date) ON DELETE CASCADE,
    FOREIGN KEY (stock_id, price_date, quote_scope)
        REFERENCES stock_price_eod(stock_id, trade_date, quote_scope),
    CHECK (price_date <= trade_date)
);

COMMENT ON TABLE sector_leading_stock IS
    'Sector leaders ranked against an index date, linked to the exact observed EOD quote.';
COMMENT ON COLUMN sector_leading_stock.price_date IS
    'Actual source quote date; it may precede the benchmark date but cannot be later.';
