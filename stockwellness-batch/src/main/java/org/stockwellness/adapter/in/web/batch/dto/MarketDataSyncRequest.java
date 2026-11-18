package org.stockwellness.adapter.in.web.batch.dto;

import java.time.LocalDate;

/** Explicit canonical market-data collection range for admin-triggered runs. */
public record MarketDataSyncRequest(
        String market,
        LocalDate startDate,
        LocalDate endDate,
        String runId,
        String ticker
) {}
