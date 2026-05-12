package org.stockwellness.application.port.out.outbox;

import java.util.List;

import org.stockwellness.domain.outbox.OutboxEvent;

public interface OutboxPort {
    void save(OutboxEvent event);
    List<OutboxEvent> findPendingEvents();
    void update(OutboxEvent event);
}
