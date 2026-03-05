package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.stockwellness.batch.common.BatchLogTemplate;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDateTime;

@Slf4j
public class StockMasterJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(BatchLogTemplate.jobStarted(jobExecution.getJobInstance().getJobName()));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long elapsedMs = DateUtil.elapsedMillis(
                jobExecution.getStartTime(),
                jobExecution.getEndTime() != null ? jobExecution.getEndTime() : DateUtil.now()
        );

        String message = BatchLogTemplate.jobFinished(
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                elapsedMs
        );

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info(message);
        } else {
            log.error(message + " | failures={}", jobExecution.getAllFailureExceptions());
        }
    }
}

@Slf4j
class StockMasterStepExecutionListener implements StepExecutionListener {

    private final String label;

    StockMasterStepExecutionListener(String label) {
        this.label = label;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(BatchLogTemplate.stepStarted(label));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long elapsedMs = DateUtil.elapsedMillis(
                stepExecution.getStartTime(),
                stepExecution.getEndTime() != null ? stepExecution.getEndTime() : DateUtil.now()
        );

        String message = BatchLogTemplate.stepFinished(label, stepExecution.getStatus(), elapsedMs);
        String counts = String.format("read=%d, write=%d, skip=%d", 
                stepExecution.getReadCount(), stepExecution.getWriteCount(), stepExecution.getSkipCount());

        log.info("{} | {}", message, counts);
        return stepExecution.getExitStatus();
    }
}
