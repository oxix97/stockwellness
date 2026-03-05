package org.stockwellness.batch.common;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 배치 로그에 Job 및 Step 정보를 MDC에 주입하여 추적성을 강화하는 리스너
 */
@Slf4j
@Component
public class BatchMdcListener implements JobExecutionListener, StepExecutionListener {

    public static final String JOB_NAME = "jobName";
    public static final String JOB_EXECUTION_ID = "jobExecutionId";
    public static final String STEP_NAME = "stepName";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        MDC.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
        MDC.put(JOB_EXECUTION_ID, String.valueOf(jobExecution.getId()));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        MDC.remove(JOB_NAME);
        MDC.remove(JOB_EXECUTION_ID);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put(STEP_NAME, stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        MDC.remove(STEP_NAME);
        return stepExecution.getExitStatus();
    }
}
