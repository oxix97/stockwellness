package org.stockwellness.batch.support.logging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Component
public class CommonBatchProgressLoggingListener implements ItemWriteListener<Object> {

    private final BatchProviderResolver providerResolver;
    private final BatchProgressLogger batchProgressLogger;
    private final BatchLogFormatter formatter;

    public CommonBatchProgressLoggingListener(
            BatchProviderResolver providerResolver,
            BatchProgressLogger batchProgressLogger,
            BatchLogFormatter formatter
    ) {
        this.providerResolver = providerResolver;
        this.batchProgressLogger = batchProgressLogger;
        this.formatter = formatter;
    }

    public void afterWrite(Chunk<?> items) {
        StepExecution stepExecution = currentStepExecution();
        if (stepExecution == null) {
            return;
        }

        JobExecution jobExecution = stepExecution.getJobExecution();
        providerResolver.findProgressSnapshotProvider(jobExecution.getJobInstance().getJobName())
                .ifPresent(provider -> {
                    // 진행 상태는 provider가 ExecutionContext에 누적 저장한다.
                    provider.updateProgress(stepExecution, items);
                    BatchProgressSnapshot snapshot = provider.getProgressSnapshot(jobExecution);
                    if (snapshot == null || !shouldLog(jobExecution, snapshot)) {
                        return;
                    }

                    Map<String, Object> fields = formatter.orderedFields();
                    fields.put("job", jobExecution.getJobInstance().getJobName());
                    fields.put("executionId", jobExecution.getId());
                    fields.put("processed", snapshot.processedCount());
                    fields.put("total", snapshot.totalCount());
                    fields.put("progress", snapshot.progressPercent());
                    fields.put(snapshot.itemIdLogKey(), snapshot.currentItemId());
                    fields.put(snapshot.itemKeyLogKey(), snapshot.currentItemKey());
                    fields.put("elapsed", formatter.formatDuration(elapsedMs(jobExecution.getStartTime())));
                    fields.put("eta", snapshot.estimatedRemainingMs() == null ? null : formatter.formatDuration(snapshot.estimatedRemainingMs()));
                    batchProgressLogger.logProgress(fields);
                    updateLastLogged(jobExecution, snapshot.processedCount());
                });
    }

    private boolean shouldLog(JobExecution jobExecution, BatchProgressSnapshot snapshot) {
        if (snapshot.processedCount() <= 0) {
            return false;
        }

        Long lastLoggedProcessedCount = getLong(jobExecution, BatchLoggingConstants.CTX_LAST_LOGGED_PROCESSED_COUNT);
        LocalDateTime lastLoggedAt = getDateTime(jobExecution, BatchLoggingConstants.CTX_LAST_LOGGED_AT);
        long countDelta = snapshot.processedCount() - (lastLoggedProcessedCount == null ? 0L : lastLoggedProcessedCount);
        boolean countThresholdReached = countDelta >= BatchLoggingConstants.PROGRESS_LOG_INTERVAL_COUNT;
        // 별도 스케줄러 없이 afterWrite 시점에만 체크하므로, 마지막 로그 시각과의 차이로 60초 조건을 판정한다.
        boolean timeThresholdReached = lastLoggedAt == null
                || elapsedSince(lastLoggedAt) >= BatchLoggingConstants.PROGRESS_LOG_INTERVAL_MS;

        if (snapshot.totalCount() != null && snapshot.processedCount() >= snapshot.totalCount()) {
            return true;
        }

        return countThresholdReached || timeThresholdReached;
    }

    private void updateLastLogged(JobExecution jobExecution, long processedCount) {
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_LAST_LOGGED_PROCESSED_COUNT, processedCount);
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_LAST_LOGGED_AT, LocalDateTime.now());
    }

    private long elapsedMs(LocalDateTime startTime) {
        if (startTime == null) {
            return 0L;
        }
        return Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    private long elapsedSince(LocalDateTime time) {
        return Duration.between(time, LocalDateTime.now()).toMillis();
    }

    private Long getLong(JobExecution jobExecution, String key) {
        Object value = jobExecution.getExecutionContext().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private LocalDateTime getDateTime(JobExecution jobExecution, String key) {
        Object value = jobExecution.getExecutionContext().get(key);
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }

    private StepExecution currentStepExecution() {
        if (StepSynchronizationManager.getContext() == null) {
            return null;
        }
        return StepSynchronizationManager.getContext().getStepExecution();
    }
}
