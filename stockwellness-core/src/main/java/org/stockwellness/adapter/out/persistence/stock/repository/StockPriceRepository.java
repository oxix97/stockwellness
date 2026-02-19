package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockPriceId;

public interface StockPriceRepository extends JpaRepository<StockPrice, StockPriceId>, StockPriceRepositoryCustom {
}
