package org.stockwellness.application.port.out.watchlist;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StockDataPort {
    Map<String, StockWellnessDetail> getStockDetails(List<String> isinCodes);

    record StockWellnessDetail(
            String isinCode,
            BigDecimal currentPrice,
            BigDecimal fluctuationRate,
            BigDecimal rsi,
            String rsiStatus,
            String aiInsight
    ) {}
}
