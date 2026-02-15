package org.stockwellness.domain.shared.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class BatchCompletedEvent {
    private final String jobName;
    private final LocalDateTime completedAt;
}
