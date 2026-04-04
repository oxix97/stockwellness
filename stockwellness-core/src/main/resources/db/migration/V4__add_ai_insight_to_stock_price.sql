-- V4: Add ai_insight column to stock_price table
ALTER TABLE stock_price ADD COLUMN IF NOT EXISTS ai_insight TEXT;
