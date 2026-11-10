CREATE INDEX IF NOT EXISTS idx_investor_trade_base_date_stock
    ON stock_investor_trade (base_date DESC, stock_id);

CREATE INDEX IF NOT EXISTS idx_investor_trade_orgn_ranking
    ON stock_investor_trade (base_date, orgn_ntby_tr_pbmn, stock_id);

CREATE INDEX IF NOT EXISTS idx_investor_trade_frgn_ranking
    ON stock_investor_trade (base_date, frgn_ntby_tr_pbmn, stock_id);
