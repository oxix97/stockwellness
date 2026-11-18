package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.EodStockPriceId;

public interface EodStockPriceRepository extends JpaRepository<EodStockPrice, EodStockPriceId> {}
