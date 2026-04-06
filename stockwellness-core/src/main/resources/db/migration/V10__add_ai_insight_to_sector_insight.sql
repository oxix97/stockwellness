-- V10: Add ai_insight column to sector_insight table
ALTER TABLE sector_insight ADD COLUMN IF NOT EXISTS ai_insight TEXT;
