package org.stockwellness.adapter.out.persistence.outbox;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;

@Component
@RequiredArgsConstructor
public class OutboxAdapter implements OutboxPort {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void save(OutboxEvent event) {
        outboxEventRepository.save(event);
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        return outboxEventRepository.findAllByStatus(OutboxStatus.PENDING);
    }

    @Override
    public void update(OutboxEvent event) {
        outboxEventRepository.save(event);
    }
}
