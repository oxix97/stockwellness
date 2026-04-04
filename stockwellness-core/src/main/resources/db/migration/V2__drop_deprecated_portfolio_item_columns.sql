-- V2: portfolio_item 테이블 레거시 컬럼 정리
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'portfolio_item' AND column_name = 'piece_count') THEN
        ALTER TABLE portfolio_item ALTER COLUMN piece_count DROP NOT NULL;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'portfolio_item' AND column_name = 'isin_code') THEN
        ALTER TABLE portfolio_item ALTER COLUMN isin_code DROP NOT NULL;
    END IF;
END $$;
