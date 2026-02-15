package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

public interface StockCustomRepository {
    Slice<Stock> searchByCondition(
            String keyword,
            MarketType marketType,
            StockStatus status,
            Pageable pageable
    );
}
