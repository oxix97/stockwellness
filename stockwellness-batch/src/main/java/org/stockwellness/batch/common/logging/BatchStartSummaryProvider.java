package org.stockwellness.batch.common.logging;

import org.springframework.batch.core.JobExecution;

public interface BatchStartSummaryProvider {
    String jobName();

    BatchStartSummary initialize(JobExecution jobExecution);
}
