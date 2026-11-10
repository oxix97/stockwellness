-- V22__create_market_weather_insight.sql
CREATE TABLE IF NOT EXISTS sector_indicator (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    sector_code VARCHAR(20) NOT NULL,
    ma20 NUMERIC(10, 4),
    ma60 NUMERIC(10, 4),
    rsi14 NUMERIC(10, 4),
    macd NUMERIC(10, 4),
    adr NUMERIC(10, 4),
    is_overheated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sector_indicator_base_date_code ON sector_indicator(base_date, sector_code);

CREATE TABLE IF NOT EXISTS sector_weather (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    sector_code VARCHAR(20) NOT NULL,
    weather_score INT NOT NULL,
    weather_state VARCHAR(20) NOT NULL,
    ai_title VARCHAR(100),
    ai_insight TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sector_weather_base_date_code ON sector_weather(base_date, sector_code);

CREATE TABLE IF NOT EXISTS market_weather (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    market_type VARCHAR(20) NOT NULL,
    weather_score INT NOT NULL,
    weather_state VARCHAR(20) NOT NULL,
    ai_summary TEXT,
    top_sectors JSONB,
    bottom_sectors JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_weather_base_date_type ON market_weather(base_date, market_type);

-- Existing installations may have created these tables outside Flyway.
ALTER TABLE sector_indicator ADD COLUMN IF NOT EXISTS ma60 numeric(10,4);
ALTER TABLE sector_indicator ADD COLUMN IF NOT EXISTS macd numeric(10,4);
ALTER TABLE sector_indicator ADD COLUMN IF NOT EXISTS updated_at timestamp(6);
ALTER TABLE sector_weather ADD COLUMN IF NOT EXISTS updated_at timestamp(6);
ALTER TABLE market_weather ADD COLUMN IF NOT EXISTS updated_at timestamp(6);
