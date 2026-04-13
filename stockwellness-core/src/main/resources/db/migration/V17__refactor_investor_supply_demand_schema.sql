-- 1. stock_price 테이블 불필요 컬럼 제거 및 개인 컬럼 추가 (운영 DB 성능 최적화를 위해 단일 ALTER TABLE 문으로 묶음)
ALTER TABLE stock_price 
    DROP COLUMN IF EXISTS pension_buying_amt,
    DROP COLUMN IF EXISTS trust_buying_amt,
    DROP COLUMN IF EXISTS etc_corp_buying_amt,
    DROP COLUMN IF EXISTS pension_buying_qty,
    DROP COLUMN IF EXISTS trust_buying_qty,
    DROP COLUMN IF EXISTS etc_corp_buying_qty,
    ADD COLUMN IF NOT EXISTS prsn_buying_amt DECIMAL(25, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prsn_buying_qty BIGINT DEFAULT 0;

COMMENT ON COLUMN stock_price.prsn_buying_amt IS '개인 순매수 금액';
COMMENT ON COLUMN stock_price.prsn_buying_qty IS '개인 순매수 수량';

-- 2. stock_investor_trade 테이블 불필요 컬럼 제거 및 개인 컬럼 추가
ALTER TABLE stock_investor_trade 
    DROP COLUMN IF EXISTS pension_buying_amt,
    DROP COLUMN IF EXISTS trust_buying_amt,
    DROP COLUMN IF EXISTS etc_corp_buying_amt,
    DROP COLUMN IF EXISTS pension_buying_qty,
    DROP COLUMN IF EXISTS trust_buying_qty,
    DROP COLUMN IF EXISTS etc_corp_buying_qty,
    ADD COLUMN IF NOT EXISTS prsn_buying_amt DECIMAL(25, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prsn_buying_qty BIGINT DEFAULT 0;

COMMENT ON COLUMN stock_investor_trade.prsn_buying_amt IS '개인 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.prsn_buying_qty IS '개인 순매수 수량';
