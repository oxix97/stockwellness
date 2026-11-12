package org.stockwellness.application.port.out.watchlist;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.stockwellness.domain.stock.Stock;

public interface StockDataPort {
    Map<Long, StockWellnessDetail> getStockDetails(List<Stock> stocks);

    record StockWellnessDetail(
            String isinCode,
            BigDecimal currentPrice,
            BigDecimal fluctuationRate,
            BigDecimal rsi,
            String rsiStatus,
            String aiInsight
    ) {}
}
