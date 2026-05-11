package org.stockwellness.domain.outbox;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = PROTECTED)
public class OutboxEvent extends AbstractEntity {

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private LocalDateTime processedAt;

    private String errorMessage;

    public static OutboxEvent create(String topic, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.topic = topic;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        return event;
    }

    public void complete() {
        this.status = OutboxStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
