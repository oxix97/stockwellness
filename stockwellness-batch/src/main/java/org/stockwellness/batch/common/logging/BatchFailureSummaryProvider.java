package org.stockwellness.batch.common.logging;

import org.springframework.batch.core.JobExecution;

public interface BatchFailureSummaryProvider {
    String jobName();

    BatchFailureSummary getFailureSummary(JobExecution jobExecution);
}
