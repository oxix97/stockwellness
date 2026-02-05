package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.StockHistory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadStockHistoryPort {
    Optional<StockHistory> findLatestHistory(String isinCode);
    List<StockHistory> loadRecentHistories(String isinCode, int limit);
    Map<String, List<StockHistory>> loadRecentHistoriesBatch(List<String> isinCodes, int limit);
}
