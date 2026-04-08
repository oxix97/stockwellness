package org.stockwellness.batch.support.logging;

import org.springframework.batch.core.JobExecution;

public interface BatchFailureSummaryProvider {
    String jobName();

    BatchFailureSummary getFailureSummary(JobExecution jobExecution);
}
