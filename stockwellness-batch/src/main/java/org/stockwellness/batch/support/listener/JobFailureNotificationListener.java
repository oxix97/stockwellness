package org.stockwellness.batch.support.listener;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.notification.NotificationPort;

/**
 * 모든 배치 Job에 공통으로 적용 가능한 실패 알림 리스너.
 * Job 실행 실패 시 NotificationPort를 통해 알림을 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobFailureNotificationListener implements JobExecutionListener {

    private final NotificationPort notificationPort;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED || jobExecution.getStatus() == BatchStatus.UNKNOWN) {
            sendNotification(jobExecution);
        }
    }

    private void sendNotification(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().toString();
        
        long executionTime = 0;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            executionTime = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toSeconds();
        }

        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        long readCount = stepExecutions.stream().mapToLong(StepExecution::getReadCount).sum();
        long writeCount = stepExecutions.stream().mapToLong(StepExecution::getWriteCount).sum();
        long skipCount = stepExecutions.stream().mapToLong(StepExecution::getSkipCount).sum();

        String errorMessage = jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        if (errorMessage.isEmpty() && jobExecution.getExitStatus() != null) {
            errorMessage = jobExecution.getExitStatus().getExitDescription();
        }

        String title = String.format("🔴 [Batch Failure] %s (ID: %d)", jobName, jobExecution.getId());
        String content = String.format(
                "작업명: %s\n" +
                "ID: %d\n" +
                "상태: %s\n" +
                "실행시간: %d초\n" +
                "처리현황: Read(%d), Write(%d), Skip(%d)\n" +
                "에러메시지: %s",
                jobName, jobExecution.getId(), status, executionTime, readCount, writeCount, skipCount, errorMessage
        );

        log.info("배치 실패 알림 전송 - Job: {}", jobName);
        notificationPort.send(title, content);
    }
}
