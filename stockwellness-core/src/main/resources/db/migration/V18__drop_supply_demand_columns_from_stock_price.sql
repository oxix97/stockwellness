-- V18__drop_supply_demand_columns_from_stock_price.sql
-- StockPrice 엔티티에서 InvestorSupplyDemand 필드가 제거됨에 따라 
-- stock_price 테이블의 수급 관련 컬럼들을 드랍합니다.

ALTER TABLE stock_price 
    DROP COLUMN IF EXISTS inst_buying_amt,
    DROP COLUMN IF EXISTS frgn_buying_amt,
    DROP COLUMN IF EXISTS prsn_buying_amt,
    DROP COLUMN IF EXISTS net_total_buying_amt,
    DROP COLUMN IF EXISTS net_institutional_buying_amt,
    DROP COLUMN IF EXISTS net_foreign_buying_amt,
    DROP COLUMN IF EXISTS inst_buying_qty,
    DROP COLUMN IF EXISTS frgn_buying_qty,
    DROP COLUMN IF EXISTS prsn_buying_qty,
    DROP COLUMN IF EXISTS total_net_qty,
    DROP COLUMN IF EXISTS net_institutional_buying_qty,
    DROP COLUMN IF EXISTS net_foreign_buying_qty;
