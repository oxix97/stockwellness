package org.stockwellness.adapter.in.batch.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class JobTimeListener implements JobExecutionListener {

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
    }
}