package org.stockwellness.application.port.out.outbox;

public interface OutboxEventPublisherPort {
    void publish(String topic, String payload);
}
