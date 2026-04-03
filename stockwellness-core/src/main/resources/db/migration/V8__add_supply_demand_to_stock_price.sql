-- V8__add_supply_demand_to_stock_price.sql
-- 종목별 일일 기관 및 외국인 순매수 금액 정보를 추가합니다.

ALTER TABLE stock_price 
ADD COLUMN net_institutional_buying_amt DECIMAL(25, 2) DEFAULT 0,
ADD COLUMN net_foreign_buying_amt DECIMAL(25, 2) DEFAULT 0;

COMMENT ON COLUMN stock_price.net_institutional_buying_amt IS '기관 순매수 금액 (단위: 원)';
COMMENT ON COLUMN stock_price.net_foreign_buying_amt IS '외국인 순매수 금액 (단위: 원)';
