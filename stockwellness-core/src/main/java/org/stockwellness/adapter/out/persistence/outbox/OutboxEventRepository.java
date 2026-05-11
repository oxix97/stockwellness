package org.stockwellness.adapter.out.persistence.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findAllByStatus(OutboxStatus status);
}
