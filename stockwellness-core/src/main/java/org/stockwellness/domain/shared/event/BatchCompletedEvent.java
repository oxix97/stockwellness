package org.stockwellness.domain.shared.event;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BatchCompletedEvent {
    private final String jobName;
    private final LocalDateTime completedAt;
}
