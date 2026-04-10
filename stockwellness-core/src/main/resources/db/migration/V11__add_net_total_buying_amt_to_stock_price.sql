-- V11__add_net_total_buying_amt_to_stock_price.sql
-- 종목별 일일 합계(외인+기관) 순매수 금액 컬럼을 추가합니다.

ALTER TABLE stock_price ADD COLUMN IF NOT EXISTS net_total_buying_amt DECIMAL(25, 2) DEFAULT 0;

COMMENT ON COLUMN stock_price.net_total_buying_amt IS '전체(외인+기관) 순매수 금액 (단위: 원)';

-- -- 기존 데이터 마이그레이션
-- UPDATE stock_price
--    SET net_total_buying_amt = COALESCE(net_institutional_buying_amt, 0) + COALESCE(net_foreign_buying_amt, 0);
