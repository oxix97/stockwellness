package org.stockwellness.batch.support.listener;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.batch.BatchResultEventPort;
import org.stockwellness.domain.shared.event.BatchResultEvent;

/**
 * 배치 작업(Job) 종료 시 결과를 캡처하여 Kafka 이벤트 발행 및 알림을 수행하는 리스너
 */
@Slf4j
@Component
public class BatchResultCaptureListener implements JobExecutionListener {

    private final BatchResultEventPort batchResultEventPort;

    public BatchResultCaptureListener(BatchResultEventPort batchResultEventPort) {
        this.batchResultEventPort = batchResultEventPort;
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

        // 실패 데이터 ID 수집 (StepExecutionContext 활용)
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

        log.info("배치 결과 캡처 완료 - 작업명: [{}], 성공여부: {}, 처리건수: {}, 실패건수: {}", 
                 jobName, isSuccess, processedCount, failedCount);
        
        batchResultEventPort.send(event);
    }
}
