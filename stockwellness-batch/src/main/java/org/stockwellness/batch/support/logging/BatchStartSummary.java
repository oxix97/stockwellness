package org.stockwellness.batch.support.logging;

public record BatchStartSummary(
        String startDate,
        String endDate,
        Long totalCount
) {
}
