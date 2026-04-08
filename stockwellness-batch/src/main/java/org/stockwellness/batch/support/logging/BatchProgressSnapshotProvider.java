package org.stockwellness.batch.support.logging;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

public interface BatchProgressSnapshotProvider {
    String jobName();

    void updateProgress(StepExecution stepExecution, Chunk<?> items);

    BatchProgressSnapshot getProgressSnapshot(JobExecution jobExecution);
}
