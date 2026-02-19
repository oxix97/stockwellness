package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;

public record LeadingStock(
        String ticker,
        String name,
        BigDecimal fluctuationRate, // 등락률
        Long tradeVolume            // 거래량
) {}