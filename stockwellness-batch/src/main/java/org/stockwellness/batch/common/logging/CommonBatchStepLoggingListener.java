package org.stockwellness.batch.common.logging;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CommonBatchStepLoggingListener implements StepExecutionListener {

    private final BatchLifecycleLogger batchLifecycleLogger;
    private final BatchLogFormatter formatter;

    public CommonBatchStepLoggingListener(BatchLifecycleLogger batchLifecycleLogger, BatchLogFormatter formatter) {
        this.batchLifecycleLogger = batchLifecycleLogger;
        this.formatter = formatter;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Map<String, Object> fields = formatter.orderedFields();
        fields.put("step", stepExecution.getStepName());
        fields.put("executionId", stepExecution.getJobExecutionId());
        fields.put("readCount", stepExecution.getReadCount());
        fields.put("writeCount", stepExecution.getWriteCount());
        fields.put("skipCount", stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount());
        fields.put("commitCount", stepExecution.getCommitCount());
        fields.put("duration", formatter.formatDuration(durationMs(stepExecution.getStartTime(), stepExecution.getEndTime())));
        batchLifecycleLogger.logStepEnd(fields);
        return stepExecution.getExitStatus();
    }

    private long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Duration.between(start, end).toMillis();
    }
}
