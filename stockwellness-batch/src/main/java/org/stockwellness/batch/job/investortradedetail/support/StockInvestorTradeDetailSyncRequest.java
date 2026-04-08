package org.stockwellness.batch.job.investortradedetail.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StockInvestorTradeDetailSyncRequest {

    /** 보정 기준일 (yyyyMMdd) */
    private String baseDate;

    /** 특정 종목 티커 (선택) */
    private String targetTicker;
}
