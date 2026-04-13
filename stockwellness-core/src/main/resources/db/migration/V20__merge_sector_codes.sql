-- Step 1: Add the unified sector_code column
ALTER TABLE stock ADD COLUMN sector_code VARCHAR(10);

-- Step 2: Migrate data using the priority logic: small -> medium -> large
-- '0000' is the KIS master's default code for "none".
-- We pick the first non-'0000', non-NULL, and non-empty code.
UPDATE stock
SET sector_code = CASE
    WHEN sector_small_code IS NOT NULL AND TRIM(sector_small_code) != '' AND sector_small_code != '0000' THEN TRIM(sector_small_code)
    WHEN sector_medium_code IS NOT NULL AND TRIM(sector_medium_code) != '' AND sector_medium_code != '0000' THEN TRIM(sector_medium_code)
    WHEN sector_large_code IS NOT NULL AND TRIM(sector_large_code) != '' AND sector_large_code != '0000' THEN TRIM(sector_large_code)
    ELSE COALESCE(TRIM(sector_large_code), '0000')
END;

-- Step 3: Remove the old redundant columns
ALTER TABLE stock DROP COLUMN sector_large_code;
ALTER TABLE stock DROP COLUMN sector_medium_code;
ALTER TABLE stock DROP COLUMN sector_small_code;
