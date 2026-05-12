package org.stockwellness.batch.support.logging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class CommonBatchJobLoggingListener implements JobExecutionListener {

    private final BatchProviderResolver providerResolver;
    private final BatchLifecycleLogger batchLifecycleLogger;
    private final BatchLogFormatter formatter;

    public CommonBatchJobLoggingListener(
            BatchProviderResolver providerResolver,
            BatchLifecycleLogger batchLifecycleLogger,
            BatchLogFormatter formatter
    ) {
        this.providerResolver = providerResolver;
        this.batchLifecycleLogger = batchLifecycleLogger;
        this.formatter = formatter;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Map<String, Object> fields = formatter.orderedFields();
        String jobName = jobExecution.getJobInstance().getJobName();
        fields.put("job", jobName);
        fields.put("executionId", jobExecution.getId());

        providerResolver.findStartSummaryProvider(jobName)
                .map(provider -> provider.initialize(jobExecution))
                .ifPresent(summary -> {
                    fields.put("startDate", summary.startDate());
                    fields.put("endDate", summary.endDate());
                    fields.put("totalCount", summary.totalCount());
                });

        fields.put("startedAt", formatter.formatDateTime(jobExecution.getStartTime()));
        batchLifecycleLogger.logStart(fields);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        Map<String, Object> fields = formatter.orderedFields();
        fields.put("job", jobName);
        fields.put("executionId", jobExecution.getId());
        fields.put("status", jobExecution.getStatus().name());

        BatchProgressSnapshot snapshot = providerResolver.findProgressSnapshotProvider(jobName)
                .map(provider -> provider.getProgressSnapshot(jobExecution))
                .orElse(null);

        fields.put("processed", snapshot != null ? snapshot.processedCount() : defaultProcessedCount(jobExecution));
        fields.put("failed", defaultFailedCount(jobExecution, snapshot));

        providerResolver.findFailureSummaryProvider(jobName)
                .map(provider -> provider.getFailureSummary(jobExecution))
                .ifPresent(summary -> {
                    fields.put("failedItemId", summary.failedItemId());
                    fields.put("failedItemKey", summary.failedItemKey());
                });

        fields.put("duration", formatter.formatDuration(durationMs(jobExecution.getStartTime(), jobExecution.getEndTime())));
        batchLifecycleLogger.logEnd(fields);
    }

    private long defaultProcessedCount(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
    }

    private long defaultFailedCount(JobExecution jobExecution, BatchProgressSnapshot snapshot) {
        long skipped = jobExecution.getStepExecutions().stream()
                .mapToLong(stepExecution ->
                        stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount())
                .sum();

        if (skipped > 0) {
            return skipped;
        }

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            return providerResolver.findFailureSummaryProvider(jobExecution.getJobInstance().getJobName())
                    .map(provider -> provider.getFailureSummary(jobExecution))
                    .filter(summary -> summary.failedItemId() != null || summary.failedItemKey() != null)
                    .map(summary -> 1L)
                    .orElse(0L);
        }

        if (snapshot != null && snapshot.totalCount() != null) {
            return Math.max(snapshot.totalCount() - snapshot.processedCount(), 0L);
        }

        return 0L;
    }

    private long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Duration.between(start, end).toMillis();
    }
}
