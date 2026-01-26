package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;

public interface StockHistoryCustomRepository {
    List<StockHistory> findRecentHistory(String isinCode, LocalDate targetDate, int limit);
}
