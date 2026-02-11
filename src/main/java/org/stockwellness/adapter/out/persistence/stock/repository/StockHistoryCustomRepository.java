package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StockHistoryCustomRepository {
    List<StockHistory> findRecentHistory(String isinCode, LocalDate targetDate, int limit);
    Map<String, List<StockHistory>> findRecentHistoryBatch(List<String> isinCodes, int limit);
}
