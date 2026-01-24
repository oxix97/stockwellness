package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.StockHistory;

import java.util.Optional;

public interface LoadStockHistoryPort {
    Optional<StockHistory> findLatestHistory(String isinCode);
}
