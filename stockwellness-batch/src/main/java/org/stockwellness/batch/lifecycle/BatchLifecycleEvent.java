package org.stockwellness.batch.lifecycle;

import java.time.LocalDateTime;

public record BatchLifecycleEvent(
        String eventId,
        BatchLifecycleEventType eventType,
        String jobName,
        Long executionId,
        String traceId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String status,
        String startDate,
        String endDate,
        Long totalCount,
        Long processedCount,
        Long failedCount,
        Double progressPercent,
        Long currentItemId,
        String currentItemKey,
        Long durationMs,
        Long estimatedRemainingMs,
        String errorMessage
) {
}
