package org.stockwellness.batch.dto;

import java.time.LocalDate;
import java.util.List;

public record DataIntegrityResponse(
    int totalCount,
    List<InvalidPriceDetail> issues
) {
    public record InvalidPriceDetail(
        String ticker,
        String name,
        LocalDate baseDate,
        String issueType
    ) {}
}
