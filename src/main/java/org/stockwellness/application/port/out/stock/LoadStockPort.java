package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.Stock;

import java.util.Optional;

public interface LoadStockPort {
    Optional<Stock> loadStockByIsinCode(String isinCode);
    boolean existsByIsinCode(String isinCode);
}
