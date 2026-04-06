package org.stockwellness.batch.common.logging;

public record BatchFailureSummary(
        Long failedItemId,
        String failedItemKey
) {
}
