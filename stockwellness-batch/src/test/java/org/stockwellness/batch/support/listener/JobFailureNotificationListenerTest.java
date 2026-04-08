package org.stockwellness.batch.support.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.stockwellness.application.port.out.notification.NotificationPort;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class JobFailureNotificationListenerTest {

    private NotificationPort notificationPort;
    private JobFailureNotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationPort = mock(NotificationPort.class);
        listener = new JobFailureNotificationListener(notificationPort);
    }

    @Test
    @DisplayName("Job이 실패하면 알림을 전송한다")
    void afterJob_Failure() {
        // given
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("testJob", 1L, 1L);
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.addFailureException(new RuntimeException("테스트 에러"));

        // when
        listener.afterJob(jobExecution);

        // then
        verify(notificationPort, times(1)).send(contains("testJob"), contains("테스트 에러"));
    }

    @Test
    @DisplayName("Job이 성공하면 알림을 전송하지 않는다")
    void afterJob_Success() {
        // given
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("testJob", 1L, 1L);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        // when
        listener.afterJob(jobExecution);

        // then
        verify(notificationPort, never()).send(anyString(), anyString());
    }
}
