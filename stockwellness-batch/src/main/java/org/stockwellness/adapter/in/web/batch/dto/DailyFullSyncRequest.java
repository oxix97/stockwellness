package org.stockwellness.adapter.in.web.batch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyFullSyncRequest {

    /** 동기화 기준일 (yyyyMMdd) */
    private String endDate;
}
