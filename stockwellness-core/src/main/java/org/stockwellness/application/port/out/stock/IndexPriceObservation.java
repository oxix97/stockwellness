package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndexPriceObservation(
        long seriesId,
        LocalDate tradeDate,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Integer advanceCount,
        Integer declineCount,
        Integer unchangedCount,
        Integer upperLimitCount,
        Integer lowerLimitCount,
        int revision,
        String dataStatus
) {}
