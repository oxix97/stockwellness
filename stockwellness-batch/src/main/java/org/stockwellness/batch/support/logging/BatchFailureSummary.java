package org.stockwellness.batch.support.logging;

public record BatchFailureSummary(
        Long failedItemId,
        String failedItemKey
) {
}
