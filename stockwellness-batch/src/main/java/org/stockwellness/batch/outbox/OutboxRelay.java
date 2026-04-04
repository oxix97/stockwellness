package org.stockwellness.batch.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.outbox.OutboxEventPublisherPort;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.domain.outbox.OutboxEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxPort outboxPort;
    private final OutboxEventPublisherPort eventPublisherPort;

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    @Transactional
    public void relay() {
        List<OutboxEvent> events = outboxPort.findPendingEvents();
        if (events.isEmpty()) return;

        log.info("Outbox 이벤트 릴레이 시작: {} 건", events.size());
        for (OutboxEvent event : events) {
            try {
                eventPublisherPort.publish(event.getTopic(), event.getPayload());
                event.complete();
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 실패 - ID: {}, Topic: {}, 사유: {}", 
                         event.getId(), event.getTopic(), e.getMessage());
                event.fail(e.getMessage());
            }
        }
    }
}
