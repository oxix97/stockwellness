package org.stockwellness.application.port.out.batch;

import org.stockwellness.domain.shared.event.BatchResultEvent;

/**
 * 배치 작업 결과를 외부(Kafka 등)로 발행하기 위한 Port 인터페이스
 */
public interface BatchResultEventPort {
    void send(BatchResultEvent event);
}
