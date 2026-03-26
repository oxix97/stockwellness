package org.stockwellness.batch.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.stockwellness.application.port.out.batch.BatchResultEventPort;
import org.stockwellness.domain.shared.event.BatchResultEvent;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchResultCaptureListenerTest {

    @Mock
    private BatchResultEventPort batchResultEventPort;

    @InjectMocks
    private BatchResultCaptureListener batchResultCaptureListener;

    @Test
    @DisplayName("배치 작업 종료 후 성공 이벤트를 발행한다")
    void afterJobSuccess() {
        // given
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = mock(JobInstance.class);
        
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("test-job");
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(10));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

        // when
        batchResultCaptureListener.afterJob(jobExecution);

        // then
        verify(batchResultEventPort).send(any(BatchResultEvent.class));
    }
}
