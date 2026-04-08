package org.stockwellness.adapter.in.web.batch.dto;

import java.time.LocalDateTime;

public record BatchJobStatusResponse(
    Long executionId,
    String jobName,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String exitCode
) {}
