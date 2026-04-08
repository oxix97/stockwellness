package org.stockwellness.batch.support.lifecycle;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.logging.BatchFailureSummary;
import org.stockwellness.batch.support.logging.BatchLogFormatter;
import org.stockwellness.batch.support.logging.BatchProgressSnapshot;
import org.stockwellness.batch.support.logging.BatchProviderResolver;
import org.stockwellness.batch.support.logging.BatchStartSummary;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BatchLifecycleEventFactory {

    private final BatchProviderResolver providerResolver;
    private final BatchLogFormatter formatter;

    public BatchLifecycleEventFactory(BatchProviderResolver providerResolver, BatchLogFormatter formatter) {
        this.providerResolver = providerResolver;
        this.formatter = formatter;
    }

    public BatchLifecycleEvent createStartedEvent(JobExecution jobExecution) {
        return create(jobExecution, BatchLifecycleEventType.STARTED);
    }

    public BatchLifecycleEvent createCompletedEvent(JobExecution jobExecution) {
        return create(jobExecution, BatchLifecycleEventType.COMPLETED);
    }

    public BatchLifecycleEvent createFailedEvent(JobExecution jobExecution) {
        return create(jobExecution, BatchLifecycleEventType.FAILED);
    }

    private BatchLifecycleEvent create(JobExecution jobExecution, BatchLifecycleEventType eventType) {
        String jobName = jobExecution.getJobInstance().getJobName();
        // beforeJob/afterJob 어디서 호출되더라도 같은 start summary를 재사용하도록 idempotent 하게 읽는다.
        BatchStartSummary startSummary = providerResolver.findStartSummaryProvider(jobName)
                .map(provider -> provider.initialize(jobExecution))
                .orElse(null);
        BatchProgressSnapshot progressSnapshot = providerResolver.findProgressSnapshotProvider(jobName)
                .map(provider -> provider.getProgressSnapshot(jobExecution))
                .orElse(null);
        BatchFailureSummary failureSummary = providerResolver.findFailureSummaryProvider(jobName)
                .map(provider -> provider.getFailureSummary(jobExecution))
                .orElse(null);

        Long processedCount = progressSnapshot != null
                ? progressSnapshot.processedCount()
                : jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();

        long failedCount = jobExecution.getStepExecutions().stream()
                .mapToLong(stepExecution ->
                        stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount())
                .sum();
        if (failedCount == 0 && jobExecution.getStatus() == BatchStatus.FAILED && failureSummary != null
                && (failureSummary.failedItemId() != null || failureSummary.failedItemKey() != null)) {
            failedCount = 1L;
        }

        return new BatchLifecycleEvent(
                UUID.randomUUID().toString(),
                eventType,
                jobName,
                jobExecution.getId(),
                traceId(jobExecution),
                jobExecution.getStartTime(),
                endedAt(jobExecution, eventType),
                jobExecution.getStatus().name(),
                startSummary == null ? null : startSummary.startDate(),
                startSummary == null ? null : startSummary.endDate(),
                startSummary == null ? null : startSummary.totalCount(),
                processedCount,
                failedCount,
                progressSnapshot == null ? null : progressSnapshot.progressPercent(),
                progressSnapshot == null ? null : progressSnapshot.currentItemId(),
                progressSnapshot == null ? null : progressSnapshot.currentItemKey(),
                durationMs(jobExecution.getStartTime(), endedAt(jobExecution, eventType)),
                progressSnapshot == null ? null : progressSnapshot.estimatedRemainingMs(),
                eventType == BatchLifecycleEventType.FAILED ? errorMessage(jobExecution) : null
        );
    }

    private String traceId(JobExecution jobExecution) {
        Object value = jobExecution.getExecutionContext().get(BatchMdcListener.TRACE_ID);
        if (value instanceof String traceId) {
            return traceId;
        }
        return jobExecution.getJobParameters().getString(BatchMdcListener.TRACE_ID);
    }

    private LocalDateTime endedAt(JobExecution jobExecution, BatchLifecycleEventType eventType) {
        if (eventType == BatchLifecycleEventType.STARTED) {
            return null;
        }
        return jobExecution.getEndTime();
    }

    private Long durationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime).toMillis();
    }

    private String errorMessage(JobExecution jobExecution) {
        String combined = jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (!combined.isBlank()) {
            return combined;
        }
        if (jobExecution.getExitStatus() == null || jobExecution.getExitStatus().getExitDescription() == null) {
            return null;
        }
        return jobExecution.getExitStatus().getExitDescription().isBlank()
                ? null
                : jobExecution.getExitStatus().getExitDescription();
    }
}
