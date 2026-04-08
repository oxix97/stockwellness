package org.stockwellness.batch.support.logging;

import org.springframework.batch.core.JobExecution;

public interface BatchStartSummaryProvider {
    String jobName();

    BatchStartSummary initialize(JobExecution jobExecution);
}
