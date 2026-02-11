package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.shared.event.BatchCompletedEvent;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobTimeListener implements JobExecutionListener {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("===========================================");
        log.info("Job Started: {}", jobExecution.getJobInstance().getJobName());
        log.info("===========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = System.currentTimeMillis() - Objects.requireNonNull(jobExecution.getStartTime()).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;

        log.info("===========================================");
        log.info("Job Finished Status: {}", jobExecution.getStatus());
        log.info("Total Duration: {} min {} sec ({} ms)", minutes, seconds, duration);
        log.info("===========================================");

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            eventPublisher.publishEvent(new BatchCompletedEvent(
                    jobExecution.getJobInstance().getJobName(),
                    LocalDateTime.now()
            ));
        }
    }
}