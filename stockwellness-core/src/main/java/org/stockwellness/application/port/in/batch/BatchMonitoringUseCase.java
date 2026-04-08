package org.stockwellness.application.port.in.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BatchMonitoringUseCase {

    List<BatchJobStatusResult> getRecentJobStatuses(String jobName, int limit);

    DataIntegrityResult checkDataIntegrity(LocalDate startDate, LocalDate endDate);

    record BatchJobStatusResult(
            Long executionId,
            String jobName,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String exitCode
    ) {
    }

    record DataIntegrityResult(
            int totalCount,
            List<InvalidPriceIssue> issues
    ) {
    }

    record InvalidPriceIssue(
            String ticker,
            String name,
            LocalDate baseDate,
            String issueType
    ) {
    }
}
