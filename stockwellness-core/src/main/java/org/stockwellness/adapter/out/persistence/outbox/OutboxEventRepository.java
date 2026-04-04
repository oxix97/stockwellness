package org.stockwellness.adapter.out.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findAllByStatus(OutboxStatus status);
}
