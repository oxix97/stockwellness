package org.stockwellness.batch.support.listener;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.listener.CompositeJobExecutionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.lifecycle.BatchLifecycleKafkaListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;

@Configuration
@RequiredArgsConstructor
public class CommonBatchListenerConfig {

    private final BatchMdcListener mdcListener;
    private final CommonBatchJobLoggingListener jobLoggingListener;
    private final BatchLifecycleKafkaListener kafkaListener;
    private final JobFailureNotificationListener notificationListener;
    private final BatchResultCaptureListener resultCaptureListener;

    @Bean
    public JobExecutionListener commonJobListener() {
        CompositeJobExecutionListener composite = new CompositeJobExecutionListener();
        composite.setListeners(Arrays.asList(
                mdcListener,
                jobLoggingListener,
                kafkaListener,
                notificationListener,
                resultCaptureListener
        ));
        return composite;
    }
}
