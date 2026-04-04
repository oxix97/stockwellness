-- V6: PortfolioItem 테이블에 매수일(purchase_date) 컬럼 추가
DO $$
BEGIN
    -- 컬럼 추가
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'portfolio_item' AND column_name = 'purchase_date') THEN
        ALTER TABLE portfolio_item ADD COLUMN purchase_date date;
        
        -- 기존 데이터는 created_at 날짜를 기준으로 채움
        UPDATE portfolio_item SET purchase_date = CAST(created_at AS date) WHERE purchase_date IS NULL;
        
        -- 향후 데이터 정합성을 위해 NOT NULL 제약 조건 추가
        ALTER TABLE portfolio_item ALTER COLUMN purchase_date SET NOT NULL;
    END IF;
END $$;
