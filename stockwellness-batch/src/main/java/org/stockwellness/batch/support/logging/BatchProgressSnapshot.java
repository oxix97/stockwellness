package org.stockwellness.batch.support.logging;

public record BatchProgressSnapshot(
        long processedCount,
        Long totalCount,
        Double progressPercent,
        Long currentItemId,
        String currentItemKey,
        String currentItemIdLabel,
        String currentItemKeyLabel,
        Long estimatedRemainingMs
) {

    public String itemIdLogKey() {
        return currentItemIdLabel == null ? "currentItemId" : currentItemIdLabel;
    }

    public String itemKeyLogKey() {
        return currentItemKeyLabel == null ? "currentItemKey" : currentItemKeyLabel;
    }
}
