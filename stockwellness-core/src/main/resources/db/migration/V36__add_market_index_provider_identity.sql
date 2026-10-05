ALTER TABLE market_index
    ADD COLUMN provider varchar(30) NOT NULL DEFAULT 'KIS',
    ADD COLUMN provider_code varchar(20),
    ADD COLUMN market_code varchar(20),
    ADD COLUMN index_kind varchar(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN source_updated_at timestamptz;

-- Existing index_code is the exact legacy source code; division and source update time were not stored.
UPDATE market_index SET provider_code = index_code;

ALTER TABLE market_index
    ALTER COLUMN provider_code SET NOT NULL,
    ADD CONSTRAINT fk_market_index_market FOREIGN KEY (market_code) REFERENCES market(code),
    ADD CONSTRAINT ck_market_index_kind CHECK (index_kind IN ('MARKET','SECTOR','OTHER')),
    ADD CONSTRAINT uq_market_index_provider_identity UNIQUE (provider, market_code, provider_code);

CREATE INDEX idx_market_index_market ON market_index(market_code);

COMMENT ON COLUMN market_index.provider_code IS
    'Provider supplied index code; retained separately from the legacy globally unique index_code.';
COMMENT ON COLUMN market_index.market_code IS
    'Filled from verified source division for newly parsed rows. Legacy rows remain NULL until source mapping is rerun.';
COMMENT ON COLUMN market_index.index_kind IS
    'Unknown legacy/source divisions remain OTHER unless their classification is directly verified.';
