package org.stockwellness.batch.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;

class BatchMdcListenerTest {

    private BatchMdcListener mdcListener;

    @BeforeEach
    void setUp() {
        mdcListener = new BatchMdcListener();
        MDC.clear();
    }

    @Test
    void testMdcDuringJobLifecycle() {
        // given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, 123L, null);

        // when (Job 시작)
        mdcListener.beforeJob(jobExecution);

        // then
        assertThat(MDC.get(BatchMdcListener.JOB_NAME)).isEqualTo("testJob");
        assertThat(MDC.get(BatchMdcListener.JOB_EXECUTION_ID)).isEqualTo("123");

        // when (Job 종료)
        mdcListener.afterJob(jobExecution);

        // then
        assertThat(MDC.get(BatchMdcListener.JOB_NAME)).isNull();
        assertThat(MDC.get(BatchMdcListener.JOB_EXECUTION_ID)).isNull();
    }

    @Test
    void testMdcDuringStepLifecycle() {
        // given
        StepExecution stepExecution = new StepExecution("testStep", null);

        // when (Step 시작)
        mdcListener.beforeStep(stepExecution);

        // then
        assertThat(MDC.get(BatchMdcListener.STEP_NAME)).isEqualTo("testStep");

        // when (Step 종료)
        mdcListener.afterStep(stepExecution);

        // then
        assertThat(MDC.get(BatchMdcListener.STEP_NAME)).isNull();
    }
}
