package org.stockwellness.batch.job.stockmaster.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.stockwellness.batch.support.BatchLogTemplate;
import org.stockwellness.global.util.DateUtil;

@Slf4j
public class StockMasterStepExecutionListener implements StepExecutionListener {

    private final String label;

    public StockMasterStepExecutionListener(String label) {
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
        String counts = String.format(
                "read=%d, write=%d, skip=%d",
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount()
        );

        log.info("{} | {}", message, counts);
        return stepExecution.getExitStatus();
    }
}
