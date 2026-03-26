package org.stockwellness.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.batch.BatchResultEventPort;
import org.stockwellness.application.port.out.notification.NotificationPort;
import org.stockwellness.domain.shared.event.BatchResultEvent;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class BatchResultCaptureListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchResultCaptureListener.class);

    private final BatchResultEventPort batchResultEventPort;
    private final NotificationPort notificationPort;

    public BatchResultCaptureListener(BatchResultEventPort batchResultEventPort, NotificationPort notificationPort) {
        this.batchResultEventPort = batchResultEventPort;
        this.notificationPort = notificationPort;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        boolean isSuccess = jobExecution.getStatus() == BatchStatus.COMPLETED;
        
        long executionTime = 0;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            executionTime = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
        }

        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        long processedCount = stepExecutions.stream().mapToLong(StepExecution::getReadCount).sum();
        long successCount = stepExecutions.stream().mapToLong(StepExecution::getWriteCount).sum();
        long failedCount = stepExecutions.stream().mapToLong(StepExecution::getProcessSkipCount).sum() 
                + stepExecutions.stream().mapToLong(StepExecution::getWriteSkipCount).sum();

        // 실패 데이터 ID 수집
        @SuppressWarnings("unchecked")
        List<String> failedIdList = stepExecutions.stream()
                .map(stepExecution -> stepExecution.getExecutionContext().get(BatchFailureItemListener.FAILED_ITEM_IDS))
                .filter(Objects::nonNull)
                .map(obj -> (List<String>) obj)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        
        String errorMessage = null;
        if (!isSuccess) {
            errorMessage = jobExecution.getAllFailureExceptions().stream()
                    .map(Throwable::getMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            
            if (errorMessage.isEmpty() && jobExecution.getExitStatus() != null 
                && !jobExecution.getExitStatus().getExitDescription().isEmpty()) {
                errorMessage = jobExecution.getExitStatus().getExitDescription();
            }
        }

        BatchResultEvent event = new BatchResultEvent(
            jobName,
            isSuccess,
            processedCount,
            successCount,
            failedCount,
            failedIdList,
            executionTime,
            errorMessage
        );

        log.info("Batch result captured for job [{}]: success={}, processed={}, failed={}", 
                 jobName, isSuccess, processedCount, failedCount);
        
        batchResultEventPort.send(event);

        if (!isSuccess) {
            sendFailureNotification(jobName, jobExecution.getStatus(), processedCount, failedCount, errorMessage);
        }
    }

    private void sendFailureNotification(String jobName, BatchStatus status, long processedCount, long failedCount, String errorMessage) {
        String title = String.format("Batch Job Failed: %s", jobName);
        String content = String.format("Status: %s\nProcessed: %d\nFailed: %d\nError: %s", 
                status, processedCount, failedCount, errorMessage);
        notificationPort.send(title, content);
    }
}
