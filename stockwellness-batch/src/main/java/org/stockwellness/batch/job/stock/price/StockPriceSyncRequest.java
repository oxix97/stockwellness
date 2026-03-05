package org.stockwellness.batch.job.stock.price;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StockPriceSyncRequest {
    /** 특정 종목 티커 (단건 처리 시 사용) */
    private String targetTicker;

    /** 수집 시작일 (yyyyMMdd) */
    private String startDate;
    
    /** 수집 종료일 (yyyyMMdd) */
    private String endDate;
}
