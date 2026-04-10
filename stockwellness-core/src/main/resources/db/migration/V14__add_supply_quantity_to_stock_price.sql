-- V14__add_supply_quantity_to_stock_price.sql
-- 종목별 일일 기관 및 외국인 순매수 수량 정보를 추가합니다.

ALTER TABLE stock_price ADD COLUMN IF NOT EXISTS net_institutional_buying_qty BIGINT DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN IF NOT EXISTS net_foreign_buying_qty BIGINT DEFAULT 0;

COMMENT ON COLUMN stock_price.net_institutional_buying_qty IS '기관 순매수 수량 (단위: 주)';
COMMENT ON COLUMN stock_price.net_foreign_buying_qty IS '외국인 순매수 수량 (단위: 주)';
