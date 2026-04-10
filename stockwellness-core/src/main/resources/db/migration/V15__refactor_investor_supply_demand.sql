-- 1. 기존 컬럼명 변경 (InvestorSupplyDemand 임베디드 구조에 맞춤)
ALTER TABLE stock_price RENAME COLUMN net_institutional_buying_amt TO inst_buying_amt;
ALTER TABLE stock_price RENAME COLUMN net_foreign_buying_amt TO frgn_buying_amt;
ALTER TABLE stock_price RENAME COLUMN net_total_buying_amt TO total_net_amt;
ALTER TABLE stock_price RENAME COLUMN net_institutional_buying_qty TO inst_buying_qty;
ALTER TABLE stock_price RENAME COLUMN net_foreign_buying_qty TO frgn_buying_qty;

-- 2. 신규 컬럼 추가 (연기금, 투신, 기타법인 및 수량 합계)
ALTER TABLE stock_price ADD COLUMN pension_buying_amt DECIMAL(25, 2) DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN trust_buying_amt DECIMAL(25, 2) DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN etc_corp_buying_amt DECIMAL(25, 2) DEFAULT 0;

ALTER TABLE stock_price ADD COLUMN pension_buying_qty BIGINT DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN trust_buying_qty BIGINT DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN etc_corp_buying_qty BIGINT DEFAULT 0;
ALTER TABLE stock_price ADD COLUMN total_net_qty BIGINT DEFAULT 0;

-- 3. 코멘트 추가 (선택 사항)
COMMENT ON COLUMN stock_price.inst_buying_amt IS '기관계 순매수 금액';
COMMENT ON COLUMN stock_price.frgn_buying_amt IS '외국인 순매수 금액';
COMMENT ON COLUMN stock_price.pension_buying_amt IS '연기금 순매수 금액';
COMMENT ON COLUMN stock_price.trust_buying_amt IS '투자신탁 순매수 금액';
COMMENT ON COLUMN stock_price.etc_corp_buying_amt IS '기타법인 순매수 금액';
COMMENT ON COLUMN stock_price.total_net_amt IS '전체 순매수 금액 합계';
