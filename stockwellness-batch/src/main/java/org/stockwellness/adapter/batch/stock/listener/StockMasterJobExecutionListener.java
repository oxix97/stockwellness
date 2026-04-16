package org.stockwellness.adapter.batch.stock.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.stockwellness.batch.support.BatchLogTemplate;
import org.stockwellness.global.util.DateUtil;

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
