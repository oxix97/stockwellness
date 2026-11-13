package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorLeadingStockCandidate(
        long stockId,
        LocalDate priceDate,
        String quoteScope,
        BigDecimal previousClose,
        BigDecimal close,
        BigDecimal tradingAmount,
        int priceRevision,
        int indexRevision
) {}
