package org.stockwellness.batch.lifecycle;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BatchLifecycleKafkaListener implements JobExecutionListener {

    private final BatchLifecycleEventPort batchLifecycleEventPort;
    private final BatchLifecycleEventFactory batchLifecycleEventFactory;

    public BatchLifecycleKafkaListener(
            BatchLifecycleEventPort batchLifecycleEventPort,
            BatchLifecycleEventFactory batchLifecycleEventFactory
    ) {
        this.batchLifecycleEventPort = batchLifecycleEventPort;
        this.batchLifecycleEventFactory = batchLifecycleEventFactory;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // lifecycle Kafka는 운영 관찰용이므로, 전송 실패가 나더라도 실제 배치 상태는 바꾸지 않는다.
        batchLifecycleEventPort.send(batchLifecycleEventFactory.createStartedEvent(jobExecution));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchLifecycleEventPort.send(batchLifecycleEventFactory.createCompletedEvent(jobExecution));
            return;
        }
        batchLifecycleEventPort.send(batchLifecycleEventFactory.createFailedEvent(jobExecution));
    }
}
