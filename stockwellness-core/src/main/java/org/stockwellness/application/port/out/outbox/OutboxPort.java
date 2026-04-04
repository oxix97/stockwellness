package org.stockwellness.application.port.out.outbox;

import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;

import java.util.List;

public interface OutboxPort {
    void save(OutboxEvent event);
    List<OutboxEvent> findPendingEvents();
    void update(OutboxEvent event);
}
