package org.stockwellness.batch.common.logging;

public record BatchStartSummary(
        String startDate,
        String endDate,
        Long totalCount
) {
}
