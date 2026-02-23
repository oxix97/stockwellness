package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class StockMasterJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("===== [StockMasterSyncJob] START | runId={} =====",
                jobExecution.getJobParameters().getLong("run.id"));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long elapsedMs = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime() != null ? jobExecution.getEndTime() : LocalDateTime.now()
        ).toMillis();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("===== [StockMasterSyncJob] COMPLETED | elapsed={}ms =====", elapsedMs);
        } else {
            log.error("===== [StockMasterSyncJob] {} | elapsed={}ms | failures={} =====",
                    jobExecution.getStatus(), elapsedMs, jobExecution.getAllFailureExceptions());
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
        log.info("[{}] Step START", label);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("[{}] Step {} | read={} write={} skip={} | elapsed={}ms",
                label,
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                Duration.between(stepExecution.getStartTime(),
                        stepExecution.getEndTime() != null
                                ? stepExecution.getEndTime() : LocalDateTime.now()).toMillis());
        return stepExecution.getExitStatus();
    }
}
