CREATE TABLE index_daily (
    index_id bigint NOT NULL REFERENCES market_index(id),
    trade_date date NOT NULL,
    open_price numeric(19,4),
    high_price numeric(19,4),
    low_price numeric(19,4),
    close_price numeric(19,4),
    volume bigint CHECK (volume >= 0),
    trading_amount numeric(25,2) CHECK (trading_amount >= 0),
    rising_issue_count integer CHECK (rising_issue_count >= 0),
    upper_limit_issue_count integer CHECK (upper_limit_issue_count >= 0),
    steady_issue_count integer CHECK (steady_issue_count >= 0),
    falling_issue_count integer CHECK (falling_issue_count >= 0),
    lower_limit_issue_count integer CHECK (lower_limit_issue_count >= 0),
    foreign_net_amount numeric(25,2),
    institution_net_amount numeric(25,2),
    price_source_api varchar(50) NOT NULL,
    supply_source_api varchar(50),
    price_status varchar(20) NOT NULL CHECK (price_status IN ('PROVISIONAL','FINAL','INCOMPLETE')),
    supply_status varchar(20) CHECK (supply_status IN ('PROVISIONAL','FINAL','INCOMPLETE')),
    collected_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    supply_collected_at timestamptz,
    corrected_at timestamptz,
    data_revision integer NOT NULL DEFAULT 1 CHECK (data_revision > 0),
    PRIMARY KEY (index_id, trade_date),
    CHECK (price_status <> 'FINAL' OR close_price IS NOT NULL),
    CHECK (high_price IS NULL OR low_price IS NULL OR high_price >= low_price),
    CHECK (supply_status IS NULL OR supply_source_api IS NOT NULL),
    CHECK (supply_status IS DISTINCT FROM 'FINAL' OR
        (foreign_net_amount IS NOT NULL AND institution_net_amount IS NOT NULL))
);
CREATE INDEX idx_index_daily_date ON index_daily(trade_date, index_id);

CREATE TABLE sector_analysis (
    index_id bigint NOT NULL,
    trade_date date NOT NULL,
    ma5 numeric(19,4),
    ma20 numeric(19,4),
    ma60 numeric(19,4),
    rsi14 numeric(10,4) CHECK (rsi14 BETWEEN 0 AND 100),
    macd numeric(19,4),
    bollinger_upper numeric(19,4),
    bollinger_mid numeric(19,4),
    bollinger_lower numeric(19,4),
    ma20_disparity numeric(12,4),
    advance_decline_ratio numeric(12,4) CHECK (advance_decline_ratio >= 0),
    advance_ratio_unavailable_reason varchar(80),
    foreign_consecutive_buy_days integer CHECK (foreign_consecutive_buy_days >= 0),
    institution_consecutive_buy_days integer CHECK (institution_consecutive_buy_days >= 0),
    is_golden_cross boolean,
    is_dead_cross boolean,
    is_overheated boolean,
    calculation_version varchar(40) NOT NULL,
    input_data_version varchar(100) NOT NULL,
    calculated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (index_id, trade_date),
    FOREIGN KEY (index_id, trade_date) REFERENCES index_daily(index_id, trade_date),
    CHECK ((advance_decline_ratio IS NULL) = (advance_ratio_unavailable_reason IS NOT NULL))
);

COMMENT ON TABLE index_daily IS
    'BenchmarkPrice와 SectorDailyDetail의 일별 원천 통합 후보. 가격·수급 상태를 별도 보존한다.';
COMMENT ON TABLE sector_analysis IS
    'SectorInsight와 SectorIndicator의 계산 결과 통합 후보. 기존 읽기 경로 전환 전까지 별도 보존한다.';
