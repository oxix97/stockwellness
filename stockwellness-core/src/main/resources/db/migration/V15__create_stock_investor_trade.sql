CREATE TABLE stock_investor_trade (
    stock_id BIGINT NOT NULL,
    base_date DATE NOT NULL,
    inst_buying_amt DECIMAL(25, 2) DEFAULT 0,
    frgn_buying_amt DECIMAL(25, 2) DEFAULT 0,
    pension_buying_amt DECIMAL(25, 2) DEFAULT 0,
    trust_buying_amt DECIMAL(25, 2) DEFAULT 0,
    etc_corp_buying_amt DECIMAL(25, 2) DEFAULT 0,
    total_net_amt DECIMAL(25, 2) DEFAULT 0,
    inst_buying_qty BIGINT DEFAULT 0,
    frgn_buying_qty BIGINT DEFAULT 0,
    pension_buying_qty BIGINT DEFAULT 0,
    trust_buying_qty BIGINT DEFAULT 0,
    etc_corp_buying_qty BIGINT DEFAULT 0,
    total_net_qty BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (stock_id, base_date),
    CONSTRAINT fk_investor_trade_stock FOREIGN KEY (stock_id) REFERENCES stock(id)
);

CREATE INDEX idx_investor_trade_lookup ON stock_investor_trade (stock_id, base_date DESC);

COMMENT ON TABLE stock_investor_trade IS '종목별 상세 투자자 매매 동향 (수급 데이터)';
COMMENT ON COLUMN stock_investor_trade.inst_buying_amt IS '기관계 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.frgn_buying_amt IS '외국인 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.pension_buying_amt IS '연기금 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.trust_buying_amt IS '투자신탁 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.etc_corp_buying_amt IS '기타법인 순매수 금액';
COMMENT ON COLUMN stock_investor_trade.total_net_amt IS '전체 순매수 금액 합계';
