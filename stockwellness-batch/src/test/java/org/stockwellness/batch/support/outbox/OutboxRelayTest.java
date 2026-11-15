package org.stockwellness.batch.support.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.outbox.OutboxEventPublisherPort;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.outbox.OutboxStatus;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    @Mock private OutboxPort outboxPort;
    @Mock private OutboxEventPublisherPort publisher;

    @Test
    void failed_publish_remains_pending_and_is_completed_after_the_next_acknowledged_attempt() {
        OutboxEvent event = OutboxEvent.create("market-data-revision", "{}");
        when(outboxPort.findPendingEvents()).thenReturn(List.of(event));
        doThrow(new IllegalStateException("broker unavailable"))
                .doNothing().when(publisher).publish("market-data-revision", "{}");
        var relay = new OutboxRelay(outboxPort, publisher);

        relay.relay();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getErrorMessage()).contains("broker unavailable");

        relay.relay();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(publisher, org.mockito.Mockito.times(2)).publish("market-data-revision", "{}");
    }
}
