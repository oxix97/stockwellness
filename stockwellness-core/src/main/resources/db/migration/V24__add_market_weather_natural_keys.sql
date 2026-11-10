-- Block concurrent writers until ranking, archival, and unique-index creation commit.
LOCK TABLE market_weather, sector_indicator, sector_weather IN SHARE ROW EXCLUSIVE MODE;

-- Keep every prior observation while retaining the most recently updated row per key.
CREATE TABLE IF NOT EXISTS sector_indicator_duplicate_archive AS SELECT * FROM sector_indicator WITH NO DATA;
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY base_date, sector_code
        ORDER BY COALESCE(updated_at, created_at) DESC NULLS LAST, id DESC) AS position
    FROM sector_indicator
), moved AS (
    DELETE FROM sector_indicator AS source USING ranked
    WHERE source.id = ranked.id AND ranked.position > 1 RETURNING source.*
)
INSERT INTO sector_indicator_duplicate_archive SELECT * FROM moved;

CREATE TABLE IF NOT EXISTS sector_weather_duplicate_archive AS SELECT * FROM sector_weather WITH NO DATA;
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY base_date, sector_code
        ORDER BY COALESCE(updated_at, created_at) DESC NULLS LAST, id DESC) AS position
    FROM sector_weather
), moved AS (
    DELETE FROM sector_weather AS source USING ranked
    WHERE source.id = ranked.id AND ranked.position > 1 RETURNING source.*
)
INSERT INTO sector_weather_duplicate_archive SELECT * FROM moved;

CREATE TABLE IF NOT EXISTS market_weather_duplicate_archive AS SELECT * FROM market_weather WITH NO DATA;
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY base_date, market_type
        ORDER BY COALESCE(updated_at, created_at) DESC NULLS LAST, id DESC) AS position
    FROM market_weather
), moved AS (
    DELETE FROM market_weather AS source USING ranked
    WHERE source.id = ranked.id AND ranked.position > 1 RETURNING source.*
)
INSERT INTO market_weather_duplicate_archive SELECT * FROM moved;

DROP INDEX IF EXISTS idx_sector_indicator_base_date_code;
DROP INDEX IF EXISTS idx_sector_weather_base_date_code;
DROP INDEX IF EXISTS idx_market_weather_base_date_type;

CREATE UNIQUE INDEX uk_sector_indicator_base_date_code
    ON sector_indicator(base_date, sector_code);

CREATE UNIQUE INDEX uk_sector_weather_base_date_code
    ON sector_weather(base_date, sector_code);

CREATE UNIQUE INDEX uk_market_weather_base_date_type
    ON market_weather(base_date, market_type);
